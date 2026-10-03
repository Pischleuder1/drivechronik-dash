package de.drivechronik.dash.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

object TeslaBleConstants {
    val SERVICE_UUID: UUID =
        UUID.fromString(
            "00000211-b2d1-43f0-9b88-960cebf8b91e"
        )

    val WRITE_UUID: UUID =
        UUID.fromString(
            "00000212-b2d1-43f0-9b88-960cebf8b91e"
        )

    val READ_UUID: UUID =
        UUID.fromString(
            "00000213-b2d1-43f0-9b88-960cebf8b91e"
        )
}

enum class TeslaBleScanStatus {
    IDLE,
    SCANNING,
    FOUND,
    NOT_FOUND,
    PERMISSION_MISSING,
    BLUETOOTH_OFF,
    ERROR
}

data class TeslaBleScanState(
    val status: TeslaBleScanStatus =
        TeslaBleScanStatus.IDLE,
    val name: String? = null,
    val address: String? = null,
    val rssi: Int? = null,
    val devicesSeen: Int = 0,
    val message: String? = null
)

class TeslaBleScanner(
    private val context: Context,
    private val onState:
        (TeslaBleScanState) -> Unit
) {

    private val handler =
        Handler(
            Looper.getMainLooper()
        )

    private var scanning = false
    private var foundTesla = false

    private val seenAddresses =
        mutableSetOf<String>()

    /*
     * Tesla BLE advertisement:
     *
     * S + 16 hex chars + C
     */
    private val teslaNamePattern =
        Regex(
            "^S[0-9a-fA-F]{16}C$"
        )

    private val stopRunnable =
        Runnable {
            stopScan()
        }

    private val callback =
        object : ScanCallback() {

            override fun onScanResult(
                callbackType: Int,
                result: ScanResult
            ) {
                handleResult(result)
            }

            override fun onBatchScanResults(
                results: MutableList<ScanResult>
            ) {
                results.forEach {
                    handleResult(it)
                }
            }

            override fun onScanFailed(
                errorCode: Int
            ) {
                Log.e(
                    "DriveChronikBLE",
                    "onScanFailed errorCode=$errorCode"
                )

                scanning = false

                onState(
                    TeslaBleScanState(
                        status =
                            TeslaBleScanStatus.ERROR,
                        devicesSeen =
                            seenAddresses.size,
                        message =
                            "Scanfehler $errorCode"
                    )
                )
            }
        }

    fun hasPermissions(): Boolean {

        val scan =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) ==
                PackageManager.PERMISSION_GRANTED

        val connect =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) ==
                PackageManager.PERMISSION_GRANTED

        return scan && connect
    }

    @SuppressLint("MissingPermission")
    fun startScan(
        durationMs: Long = 12_000L
    ) {

        if (scanning) {
            return
        }

        if (!hasPermissions()) {

            onState(
                TeslaBleScanState(
                    status =
                        TeslaBleScanStatus
                            .PERMISSION_MISSING
                )
            )

            return
        }

        val manager =
            context.getSystemService(
                BluetoothManager::class.java
            )

        val adapter =
            manager?.adapter

        if (
            adapter == null ||
            !adapter.isEnabled
        ) {

            onState(
                TeslaBleScanState(
                    status =
                        TeslaBleScanStatus
                            .BLUETOOTH_OFF
                )
            )

            return
        }

        val scanner =
            adapter.bluetoothLeScanner

        if (scanner == null) {

            onState(
                TeslaBleScanState(
                    status =
                        TeslaBleScanStatus.ERROR,
                    message =
                        "BLE Scanner nicht verfügbar"
                )
            )

            return
        }

        seenAddresses.clear()
        foundTesla = false
        scanning = true

        onState(
            TeslaBleScanState(
                status =
                    TeslaBleScanStatus.SCANNING
            )
        )

        val settings =
            ScanSettings.Builder()
                .setScanMode(
                    ScanSettings.SCAN_MODE_LOW_LATENCY
                )
                .setCallbackType(
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES
                )
                .setReportDelay(0L)
                .build()

        Log.d(
            "DriveChronikBLE",
            "START scan enabled=${adapter.isEnabled} durationMs=$durationMs"
        )

        scanner.startScan(
            null,
            settings,
            callback
        )

        handler.removeCallbacks(
            stopRunnable
        )

        handler.postDelayed(
            stopRunnable,
            durationMs
        )
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {

        if (!scanning) {
            return
        }

        val manager =
            context.getSystemService(
                BluetoothManager::class.java
            )

        Log.d(
            "DriveChronikBLE",
            "STOP scan devicesSeen=${seenAddresses.size} foundTesla=$foundTesla"
        )

        manager
            ?.adapter
            ?.bluetoothLeScanner
            ?.stopScan(callback)

        scanning = false

        handler.removeCallbacks(
            stopRunnable
        )

        if (!foundTesla) {

            onState(
                TeslaBleScanState(
                    status =
                        TeslaBleScanStatus
                            .NOT_FOUND,
                    devicesSeen =
                        seenAddresses.size
                )
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun handleResult(
        result: ScanResult
    ) {

        val address =
            result.device.address

        seenAddresses.add(address)

        val record =
            result.scanRecord

        val name =
            record?.deviceName
                ?: result.device.name

        Log.d(
            "DriveChronikBLE",
            "BLE name=${name ?: "<ohne Name>"} address=$address rssi=${result.rssi}"
        )

        val matchesName =
            name
                ?.matches(
                    teslaNamePattern
                )
                ?: false

        val matchesService =
            record
                ?.serviceUuids
                ?.any {
                    it.uuid ==
                        TeslaBleConstants
                            .SERVICE_UUID
                }
                ?: false

        if (
            matchesName ||
            matchesService
        ) {

            foundTesla = true

            onState(
                TeslaBleScanState(
                    status =
                        TeslaBleScanStatus.FOUND,
                    name = name,
                    address = address,
                    rssi = result.rssi,
                    devicesSeen =
                        seenAddresses.size
                )
            )

            stopScan()

            return
        }

        onState(
            TeslaBleScanState(
                status =
                    TeslaBleScanStatus.SCANNING,
                devicesSeen =
                    seenAddresses.size
            )
        )
    }
}
