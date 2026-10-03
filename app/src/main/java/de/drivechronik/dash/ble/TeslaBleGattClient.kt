package de.drivechronik.dash.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothStatusCodes
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

enum class TeslaGattStatus {
    IDLE,
    CONNECTING,
    CONNECTED,
    DISCOVERING_SERVICES,
    SERVICE_READY,
    SUBSCRIBING,
    READY,
    DISCONNECTED,
    ERROR
}

data class TeslaGattState(
    val status: TeslaGattStatus = TeslaGattStatus.IDLE,
    val address: String? = null,
    val mtu: Int = 23,
    val serviceFound: Boolean = false,
    val writeFound: Boolean = false,
    val readFound: Boolean = false,
    val message: String? = null
)

class TeslaBleGattClient(
    private val context: Context,
    private val onState: (TeslaGattState) -> Unit,
    private val onData: (ByteArray) -> Unit = {}
) {

    companion object {
        private val CCCD_UUID =
            UUID.fromString(
                "00002902-0000-1000-8000-00805f9b34fb"
            )

        private const val TAG =
            "DriveChronikGATT"

        private const val REQUESTED_MTU =
            517
    }

    private var gatt: BluetoothGatt? = null

    private var currentAddress: String? = null

    private var currentMtu = 23

    private var readCharacteristic:
        BluetoothGattCharacteristic? = null

    private var writeCharacteristic:
        BluetoothGattCharacteristic? = null

    private val frameDecoder =
        TeslaBleFrameDecoder()

    private val txQueue =
        java.util.ArrayDeque<ByteArray>()

    private val txLock =
        Any()

    private var writeInFlight =
        false

    private var transportReady =
        false

    private fun hasConnectPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    fun connect(address: String) {

        if (!hasConnectPermission()) {
            emitError(
                address,
                "BLUETOOTH_CONNECT fehlt"
            )
            return
        }

        if (
            currentAddress == address &&
            gatt != null
        ) {
            return
        }

        close()

        val manager =
            context.getSystemService(
                BluetoothManager::class.java
            )

        val adapter =
            manager?.adapter

        if (adapter == null) {
            emitError(
                address,
                "Bluetooth Adapter nicht verfügbar"
            )
            return
        }

        val device =
            try {
                adapter.getRemoteDevice(address)
            }
            catch (e: IllegalArgumentException) {
                emitError(
                    address,
                    "Ungültige BLE-Adresse"
                )
                return
            }

        currentAddress = address

        Log.d(
            TAG,
            "CONNECT address=$address"
        )

        onState(
            TeslaGattState(
                status =
                    TeslaGattStatus.CONNECTING,
                address = address
            )
        )

        gatt =
            device.connectGatt(
                context,
                false,
                callback,
                BluetoothDevice.TRANSPORT_LE
            )
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {

        Log.d(
            TAG,
            "DISCONNECT address=$currentAddress"
        )

        gatt?.disconnect()
    }

    @SuppressLint("MissingPermission")
    fun close() {

        try {
            gatt?.close()
        }
        catch (_: Exception) {
        }

        gatt = null
        readCharacteristic = null
        writeCharacteristic = null

        frameDecoder.reset()

        synchronized(txLock) {
            txQueue.clear()
            writeInFlight = false
        }

        transportReady = false

        currentMtu = 23
        currentAddress = null
    }

    private val callback =
        object : BluetoothGattCallback() {

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(
                gatt: BluetoothGatt,
                status: Int,
                newState: Int
            ) {

                Log.d(
                    TAG,
                    "CONNECTION status=$status newState=$newState"
                )

                if (
                    status != BluetoothGatt.GATT_SUCCESS
                ) {

                    emitError(
                        gatt.device.address,
                        "GATT Verbindungsfehler $status"
                    )

                    gatt.close()
                    this@TeslaBleGattClient.gatt =
                        null

                    return
                }

                when (newState) {

                    BluetoothProfile.STATE_CONNECTED -> {

                        currentAddress =
                            gatt.device.address

                        onState(
                            TeslaGattState(
                                status =
                                    TeslaGattStatus.CONNECTED,
                                address =
                                    currentAddress
                            )
                        )

                        gatt.requestConnectionPriority(
                            BluetoothGatt
                                .CONNECTION_PRIORITY_HIGH
                        )

                        onState(
                            TeslaGattState(
                                status =
                                    TeslaGattStatus
                                        .DISCOVERING_SERVICES,
                                address =
                                    currentAddress
                            )
                        )

                        val started =
                            gatt.discoverServices()

                        Log.d(
                            TAG,
                            "DISCOVER_SERVICES started=$started"
                        )

                        if (!started) {
                            emitError(
                                currentAddress,
                                "Service Discovery konnte nicht gestartet werden"
                            )
                        }
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {

                        Log.d(
                            TAG,
                            "DISCONNECTED"
                        )

                        onState(
                            TeslaGattState(
                                status =
                                    TeslaGattStatus.DISCONNECTED,
                                address =
                                    currentAddress,
                                mtu =
                                    currentMtu
                            )
                        )

                        gatt.close()

                        this@TeslaBleGattClient.gatt =
                            null

                        readCharacteristic = null
                        writeCharacteristic = null
                    }
                }
            }

            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(
                gatt: BluetoothGatt,
                status: Int
            ) {

                Log.d(
                    TAG,
                    "SERVICES_DISCOVERED status=$status count=${gatt.services.size}"
                )

                if (
                    status != BluetoothGatt.GATT_SUCCESS
                ) {
                    emitError(
                        currentAddress,
                        "Service Discovery Fehler $status"
                    )
                    return
                }

                val service =
                    gatt.getService(
                        TeslaBleConstants.SERVICE_UUID
                    )

                if (service == null) {

                    emitError(
                        currentAddress,
                        "Tesla Service 0x211 nicht gefunden"
                    )

                    return
                }

                val write =
                    service.getCharacteristic(
                        TeslaBleConstants.WRITE_UUID
                    )

                val read =
                    service.getCharacteristic(
                        TeslaBleConstants.READ_UUID
                    )

                writeCharacteristic = write
                readCharacteristic = read

                Log.d(
                    TAG,
                    "TESLA_SERVICE " +
                        "write=${write != null} " +
                        "read=${read != null} " +
                        "writeProps=0x${write?.properties?.toString(16)} " +
                        "readProps=0x${read?.properties?.toString(16)}"
                )

                onState(
                    TeslaGattState(
                        status =
                            TeslaGattStatus.SERVICE_READY,
                        address =
                            currentAddress,
                        mtu =
                            currentMtu,
                        serviceFound = true,
                        writeFound =
                            write != null,
                        readFound =
                            read != null
                    )
                )

                if (
                    write == null ||
                    read == null
                ) {

                    emitError(
                        currentAddress,
                        "Tesla Read/Write Characteristic fehlt"
                    )

                    return
                }

                /*
                 * Tesla verlangt für TX "write with response".
                 * Das prüfen wir bereits hier.
                 */
                val supportsWriteWithResponse =
                    write.properties and
                        BluetoothGattCharacteristic
                            .PROPERTY_WRITE != 0

                if (!supportsWriteWithResponse) {

                    emitError(
                        currentAddress,
                        "Tesla Write Characteristic unterstützt kein Write-with-response"
                    )

                    return
                }

                /*
                 * Erst MTU versuchen, danach RX abonnieren.
                 */
                val mtuStarted =
                    gatt.requestMtu(
                        REQUESTED_MTU
                    )

                Log.d(
                    TAG,
                    "REQUEST_MTU requested=$REQUESTED_MTU started=$mtuStarted"
                )

                if (!mtuStarted) {
                    enableVehicleRx(gatt)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic:
                    BluetoothGattCharacteristic,
                status: Int
            ) {

                if (
                    characteristic.uuid !=
                    TeslaBleConstants.WRITE_UUID
                ) {
                    return
                }

                Log.d(
                    TAG,
                    "TX_WRITE_DONE status=$status"
                )

                if (
                    status !=
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    synchronized(txLock) {
                        txQueue.clear()
                        writeInFlight = false
                    }

                    emitError(
                        currentAddress,
                        "Tesla BLE Write fehlgeschlagen: $status"
                    )

                    return
                }

                synchronized(txLock) {
                    writeInFlight = false
                }

                pumpTxQueue()
            }


            override fun onMtuChanged(
                gatt: BluetoothGatt,
                mtu: Int,
                status: Int
            ) {

                Log.d(
                    TAG,
                    "MTU_CHANGED mtu=$mtu status=$status"
                )

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {
                    currentMtu = mtu
                }

                enableVehicleRx(gatt)
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {

                if (
                    descriptor.uuid != CCCD_UUID
                ) {
                    return
                }

                Log.d(
                    TAG,
                    "CCCD_WRITE status=$status mtu=$currentMtu"
                )

                if (
                    status ==
                    BluetoothGatt.GATT_SUCCESS
                ) {

                    transportReady = true

                    Log.d(
                        TAG,
                        "TRANSPORT_READY mtu=$currentMtu"
                    )

                    onState(
                        TeslaGattState(
                            status =
                                TeslaGattStatus.READY,
                            address =
                                currentAddress,
                            mtu =
                                currentMtu,
                            serviceFound = true,
                            writeFound = true,
                            readFound = true
                        )
                    )
                }
                else {

                    emitError(
                        currentAddress,
                        "RX-Aktivierung fehlgeschlagen: $status"
                    )
                }
            }

            @Deprecated(
                "Legacy callback for Android 12"
            )
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic:
                    BluetoothGattCharacteristic
            ) {

                if (
                    characteristic.uuid ==
                    TeslaBleConstants.READ_UUID
                ) {

                    val data =
                        characteristic.value
                            ?: return

                    handleIncoming(data)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic:
                    BluetoothGattCharacteristic,
                value: ByteArray
            ) {

                if (
                    characteristic.uuid ==
                    TeslaBleConstants.READ_UUID
                ) {
                    handleIncoming(value)
                }
            }
        }

    @SuppressLint("MissingPermission")
    private fun enableVehicleRx(
        gatt: BluetoothGatt
    ) {

        val read =
            readCharacteristic
                ?: run {
                    emitError(
                        currentAddress,
                        "Tesla Read Characteristic fehlt"
                    )
                    return
                }

        val properties =
            read.properties

        val supportsIndicate =
            properties and
                BluetoothGattCharacteristic
                    .PROPERTY_INDICATE != 0

        val supportsNotify =
            properties and
                BluetoothGattCharacteristic
                    .PROPERTY_NOTIFY != 0

        Log.d(
            TAG,
            "RX properties=0x${properties.toString(16)} " +
                "indicate=$supportsIndicate " +
                "notify=$supportsNotify"
        )

        if (
            !supportsIndicate &&
            !supportsNotify
        ) {

            emitError(
                currentAddress,
                "Tesla RX unterstützt weder Indicate noch Notify"
            )

            return
        }

        onState(
            TeslaGattState(
                status =
                    TeslaGattStatus.SUBSCRIBING,
                address =
                    currentAddress,
                mtu =
                    currentMtu,
                serviceFound = true,
                writeFound = true,
                readFound = true
            )
        )

        val localEnabled =
            gatt.setCharacteristicNotification(
                read,
                true
            )

        Log.d(
            TAG,
            "SET_RX_NOTIFICATION localEnabled=$localEnabled"
        )

        if (!localEnabled) {

            emitError(
                currentAddress,
                "Lokale RX-Aktivierung fehlgeschlagen"
            )

            return
        }

        val cccd =
            read.getDescriptor(
                CCCD_UUID
            )

        if (cccd == null) {

            emitError(
                currentAddress,
                "CCCD 0x2902 fehlt"
            )

            return
        }

        val value =
            if (supportsIndicate)
                BluetoothGattDescriptor
                    .ENABLE_INDICATION_VALUE
            else
                BluetoothGattDescriptor
                    .ENABLE_NOTIFICATION_VALUE

        val started =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                gatt.writeDescriptor(
                    cccd,
                    value
                ) ==
                    BluetoothGatt.GATT_SUCCESS
            }
            else {

                @Suppress("DEPRECATION")
                cccd.value = value

                @Suppress("DEPRECATION")
                gatt.writeDescriptor(
                    cccd
                )
            }

        Log.d(
            TAG,
            "WRITE_CCCD indicate=$supportsIndicate started=$started"
        )

        if (!started) {

            emitError(
                currentAddress,
                "CCCD-Schreibvorgang konnte nicht gestartet werden"
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun sendRoutableMessage(
        message: ByteArray
    ): Boolean {

        if (!hasConnectPermission()) {
            return false
        }

        if (!transportReady) {

            Log.w(
                TAG,
                "TX rejected: transport not ready"
            )

            return false
        }

        if (
            gatt == null ||
            writeCharacteristic == null
        ) {

            Log.w(
                TAG,
                "TX rejected: GATT/characteristic unavailable"
            )

            return false
        }

        val frame =
            try {
                TeslaBleFrameCodec.encode(
                    message
                )
            }
            catch (
                e: IllegalArgumentException
            ) {

                Log.e(
                    TAG,
                    "TX framing failed: ${e.message}"
                )

                return false
            }

        /*
         * Konservative ATT-Nutzlast:
         * ausgehandelte MTU minus 3 Byte
         * ATT-Write-Overhead.
         */
        val maxChunkSize =
            (currentMtu - 3)
                .coerceAtLeast(20)

        val chunks =
            TeslaBleFrameCodec.chunk(
                frame,
                maxChunkSize
            )

        synchronized(txLock) {

            chunks.forEach {
                txQueue.addLast(it)
            }
        }

        Log.d(
            TAG,
            "TX_MESSAGE message=${message.size} " +
                "frame=${frame.size} " +
                "mtu=$currentMtu " +
                "chunk=$maxChunkSize " +
                "chunks=${chunks.size}"
        )

        pumpTxQueue()

        return true
    }


    @SuppressLint("MissingPermission")
    private fun pumpTxQueue() {

        val localGatt =
            gatt ?: return

        val characteristic =
            writeCharacteristic ?: return

        val chunk =
            synchronized(txLock) {

                if (writeInFlight) {
                    return
                }

                val next =
                    txQueue.pollFirst()
                        ?: return

                writeInFlight = true

                next
            }

        val started =
            if (
                Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.TIRAMISU
            ) {

                localGatt.writeCharacteristic(
                    characteristic,
                    chunk,
                    BluetoothGattCharacteristic
                        .WRITE_TYPE_DEFAULT
                ) ==
                    BluetoothStatusCodes.SUCCESS
            }
            else {

                @Suppress("DEPRECATION")
                characteristic.writeType =
                    BluetoothGattCharacteristic
                        .WRITE_TYPE_DEFAULT

                @Suppress("DEPRECATION")
                characteristic.value =
                    chunk

                @Suppress("DEPRECATION")
                localGatt.writeCharacteristic(
                    characteristic
                )
            }

        Log.d(
            TAG,
            "TX_CHUNK bytes=${chunk.size} started=$started"
        )

        if (!started) {

            synchronized(txLock) {
                txQueue.clear()
                writeInFlight = false
            }

            emitError(
                currentAddress,
                "Tesla BLE Write konnte nicht gestartet werden"
            )
        }
    }


    private fun handleIncoming(
        data: ByteArray
    ) {

        Log.d(
            TAG,
            "RX_CHUNK bytes=${data.size} hex=${data.toHex(80)}"
        )

        try {

            val messages =
                frameDecoder.feed(data)

            if (messages.isEmpty()) {

                Log.d(
                    TAG,
                    "RX_BUFFER buffered=${frameDecoder.bufferedByteCount()}"
                )

                return
            }

            messages.forEach { message ->

                Log.d(
                    TAG,
                    "RX_MESSAGE bytes=${message.size} hex=${message.toHex(80)}"
                )

                onData(
                    message.copyOf()
                )
            }
        }
        catch (e: IllegalArgumentException) {

            frameDecoder.reset()

            emitError(
                currentAddress,
                "Ungültiger Tesla BLE Frame: ${e.message}"
            )
        }
    }


    private fun emitError(
        address: String?,
        message: String
    ) {

        Log.e(
            TAG,
            message
        )

        onState(
            TeslaGattState(
                status =
                    TeslaGattStatus.ERROR,
                address = address,
                mtu =
                    currentMtu,
                message =
                    message
            )
        )
    }

    private fun ByteArray.toHex(
        maxBytes: Int
    ): String {

        return take(maxBytes)
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }
    }
}
