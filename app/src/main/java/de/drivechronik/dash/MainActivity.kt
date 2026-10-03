package de.drivechronik.dash

import android.graphics.Paint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import de.drivechronik.dash.ble.TeslaBleScanState
import de.drivechronik.dash.ble.TeslaBleScanStatus
import de.drivechronik.dash.ble.TeslaBleScanner
import de.drivechronik.dash.ble.TeslaBleGattClient
import de.drivechronik.dash.ble.TeslaRoutableMessageCodec
import de.drivechronik.dash.ble.TeslaGattState
import de.drivechronik.dash.ble.TeslaGattStatus
import de.drivechronik.dash.ble.TeslaBleSessionController
import de.drivechronik.dash.ble.TeslaKeyStore
import de.drivechronik.dash.ble.TeslaLiveProtocolSettings
import de.drivechronik.dash.ble.TeslaLiveProtocolConfig
import de.drivechronik.dash.ble.TeslaProtocolSessionStatus
import com.tesla.generated.universalmessage.UniversalMessage
import de.drivechronik.dash.ble.TeslaVehicleDataRouter
import de.drivechronik.dash.data.TeslaDashboardRepository
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.drivechronik.dash.theme.DriveChronikDashTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val DashBackground = Color(0xFF000000)
private val DashPanel = Color(0xFF111315)
private val DashLine = Color(0xFF292C30)
private val DashText = Color(0xFFF4F4F4)
private val DashMuted = Color(0xFF9A9DA2)
private val DashRed = Color(0xFFE82127)
private val DashBlue = Color(0xFF3E8BFF)
private val DashGreen = Color(0xFF3ECF6E)

data class DashboardUiState(
    val speed: Float = 100f,
    val speedLimit: Int = 100,
    val powerKw: Float = 15.3f,
    val remainingKwh: Float = 43.7f,
    val usableCapacityKwh: Float = 60f,
    val rangeKm: Int = 308,
    val leftIndicator: Boolean = false,
    val rightIndicator: Boolean = false,
    val lowBeam: Boolean = true,
    val highBeam: Boolean = false,
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        WindowCompat.setDecorFitsSystemWindows(
            window,
            false
        )

        WindowCompat
            .getInsetsController(
                window,
                window.decorView
            )
            .apply {
                hide(
                    WindowInsetsCompat.Type.systemBars()
                )

                systemBarsBehavior =
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }

        setContent {
            DriveChronikDashTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DashBackground
                ) {
                    DriveChronikDashboard()
                }
            }
        }
    }
}

@Composable
private fun DriveChronikDashboard() {

    var speed by remember {
        mutableFloatStateOf(96f)
    }

    var powerKw by remember {
        mutableFloatStateOf(15.3f)
    }

    var remainingKwh by remember {
        mutableFloatStateOf(43.8f)
    }

    var speedLimit by remember {
        mutableIntStateOf(100)
    }

    var tick by remember {
        mutableIntStateOf(0)
    }

    LaunchedEffect(Unit) {

        val limits =
            listOf(
                50,
                70,
                100,
                120
            )

        var limitIndex = 2

        while (true) {

            delay(400)

            tick++

            /*
             * Ruhige Simulation für die UI.
             * Später werden diese Werte vollständig
             * durch Tesla-BLE-Daten ersetzt.
             */

            speed =
                96f +
                sin(tick * 0.055f) * 11f

            powerKw =
                10f +
                sin(tick * 0.10f) * 18f

            /*
             * Restenergie sehr langsam reduzieren.
             */
            remainingKwh =
                (remainingKwh - 0.0008f)
                    .coerceAtLeast(0f)

            /*
             * Nur gelegentlich das Tempolimit ändern.
             */
            if (tick % 100 == 0) {

                limitIndex =
                    (limitIndex + 1) %
                    limits.size

                speedLimit =
                    limits[limitIndex]
            }
        }
    }

    val rangeKm =
        (
            remainingKwh *
            1000f /
            142f
        ).roundToInt()

    DashboardContent(
        state = DashboardUiState(
            speed = speed,
            speedLimit = speedLimit,
            powerKw = powerKw,
            remainingKwh = remainingKwh,
            usableCapacityKwh = 60f,
            rangeKm = rangeKm,

            // Blinkersimulation
            leftIndicator =
                (tick % 120 in 10..34) &&
                (tick % 4 < 2),

            rightIndicator =
                (tick % 120 in 72..96) &&
                (tick % 4 < 2),

            // Lichtsimulation
            lowBeam =
                tick % 120 !in 48..62,

            highBeam =
                tick % 120 in 48..62
        )
    )
}

@Composable
private fun DashboardContent(
    state: DashboardUiState
) {

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(DashBackground)
                .padding(
                    horizontal = 8.dp,
                    vertical = 5.dp
                )
    ) {

        DashboardHeader()

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashLine)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
        ) {

            /*
             * Linker Bereich:
             * Tacho + Tempolimit + Restenergie
             */
            Column(
                modifier =
                    Modifier
                        .weight(1.80f)
                        .fillMaxHeight()
            ) {

                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                ) {

                    Speedometer(
                        speed = state.speed,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    top = 2.dp,
                                    end = 8.dp,
                                    bottom = 2.dp
                                )
                    )

                    SpeedLimitSign(
                        limit = state.speedLimit,
                        modifier =
                            Modifier
                                .align(
                                    Alignment.TopEnd
                                )
                                .padding(
                                    top = 10.dp,
                                    end = 14.dp
                                )
                    )
                }

            }

            Box(
                modifier =
                    Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(DashLine)
            )

            /*
             * Rechte Seite:
             * nur Leistung und Reichweite
             */
            Column(
                modifier =
                    Modifier
                        .weight(1.18f)
                        .fillMaxHeight()
                        .padding(
                            start = 6.dp,
                            top = 7.dp,
                            bottom = 7.dp
                        ),
                verticalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {

                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {

                    KpiCard(
                        title = "LEISTUNG",
                        value =
                            String.format(
                                "%.1f",
                                state.powerKw
                            ),
                        unit = "kW",
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                        valueColor =
                            if (state.powerKw < -0.5f)
                                Color(0xFF4B9CFF)
                            else
                                DashText
                    )

                    KpiCard(
                        title = "REICHWEITE",
                        value =
                            state.rangeKm.toString(),
                        unit = "km",
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                    )
                }

                RestEnergyPanel(
                    remainingKwh =
                        state.remainingKwh,
                    maxKwh =
                        state.usableCapacityKwh,
                    compact = true
                )

                VehicleStatusBar(
                    leftIndicator =
                        state.leftIndicator,
                    rightIndicator =
                        state.rightIndicator,
                    lowBeam =
                        state.lowBeam,
                    highBeam =
                        state.highBeam,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                )
            }
        }
    }
}

@Composable
private fun DashboardHeader() {

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "DRIVECHRONIK DASH",
            color = DashText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp
        )

        Spacer(
            modifier =
                Modifier.weight(1f)
        )

        TeslaBleHeaderStatus()
    }
}


private fun hasBlePermissions(
    context: Context
): Boolean {

    return (
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_SCAN
        ) ==
            PackageManager.PERMISSION_GRANTED
        &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) ==
            PackageManager.PERMISSION_GRANTED
    )
}


@Composable
private fun TeslaBleHeaderStatus() {

    val context =
        LocalContext.current


    /*
     * Lokale Tesla-Konfiguration.
     *
     * Sie bleibt standardmäßig deaktiviert.
     */
    val liveProtocolSettings =
        remember {

            TeslaLiveProtocolSettings(
                context.applicationContext
            )
        }


    var liveProtocolConfig by
        remember {

            mutableStateOf(
                liveProtocolSettings.load()
            )
        }


    /*
     * Der KeyStore wird nur geprüft.
     * Hier wird noch kein Schlüssel erzeugt.
     */
    var keyPresent by
        remember {

            mutableStateOf(
                runCatching {
                    TeslaKeyStore.hasKey()
                }
                    .getOrDefault(false)
            )
        }


    val keyFingerprint =
        remember(
            keyPresent
        ) {

            if (keyPresent) {
                teslaKeyFingerprint()
            }
            else {
                null
            }
        }


    var keyError by
        remember {

            mutableStateOf<String?>(
                null
            )
        }


    var showTeslaSetup by
        remember {

            mutableStateOf(false)
        }


    /*
     * Ohne
     *
     * - gültige VIN
     * - bestätigte Key-Einschreibung
     * - Live-Freigabe
     * - vorhandenen lokalen Key
     *
     * existiert KEIN Session-Controller.
     */
    val sessionController =
        remember(
            liveProtocolConfig,
            keyPresent
        ) {

            if (
                liveProtocolConfig
                    .canStartSession &&
                keyPresent
            ) {

                TeslaBleSessionController(
                    vin =
                        liveProtocolConfig
                            .normalizedVin,

                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,

                    keyPair =
                        TeslaKeyStore
                            .getOrCreateKeyPair()
                )
            }
            else {
                null
            }
        }


    /*
     * Der GATT-Client wird nur einmal erzeugt.
     * Sein Callback muss trotzdem immer den
     * AKTUELLEN Session-Controller sehen.
     */
    val currentSessionController =
        rememberUpdatedState(
            sessionController
        )


    var permissionGranted by
        remember {
            mutableStateOf(
                hasBlePermissions(
                    context
                )
            )
        }

    var scanState by
        remember {
            mutableStateOf(
                TeslaBleScanState()
            )
        }

    var gattState by
        remember {
            mutableStateOf(
                TeslaGattState()
            )
        }

    val scanner =
        remember {
            TeslaBleScanner(
                context.applicationContext
            ) {
                scanState = it
            }
        }

    val gattClient =
        remember {
            TeslaBleGattClient(
                context =
                    context.applicationContext,
                onState = {
                    gattState = it
                },
                onData = { bytes ->

                    try {

                        val message =
                            TeslaRoutableMessageCodec
                                .decode(bytes)

                        val uuid =
                            message
                                .getUuid()
                                .toByteArray()
                                .joinToString("") {
                                    "%02x".format(
                                        it.toInt() and 0xff
                                    )
                                }

                        val toDomain =
                            message
                                .getToDestination()
                                .getDomain()

                        val fromDomain =
                            message
                                .getFromDestination()
                                .getDomain()

                        val payloadBytes =
                            message
                                .getProtobufMessageAsBytes()
                                .size()

                        Log.d(
                            "DriveChronikPROTO",
                            "RX RoutableMessage " +
                                "bytes=${bytes.size} " +
                                "to=$toDomain " +
                                "from=$fromDomain " +
                                "payload=$payloadBytes " +
                                "uuid=$uuid"
                        )


                        /*
                         * Erster echter Datenpfad:
                         *
                         * Nur unverschlüsselte
                         * CarServer-Antworten werden
                         * hier direkt ausgewertet.
                         *
                         * AES-GCM-Antworten brauchen
                         * zuerst den Session-Controller
                         * und werden deshalb hier
                         * ausdrücklich übersprungen.
                         */
                        val hasSessionInfo =
                            message
                                .getSessionInfo()
                                .size() > 0


                        if (hasSessionInfo) {

                            val controller =
                                currentSessionController
                                    .value


                            if (
                                controller != null &&
                                controller.status() ==
                                TeslaProtocolSessionStatus
                                    .HANDSHAKE_PENDING
                            ) {

                                try {

                                    val state =
                                        controller
                                            .acceptHandshakeResponse(
                                                message
                                            )


                                    Log.d(
                                        "DriveChronikSESSION",
                                        "Session READY " +
                                            "counter=${state.currentCounter()}"
                                    )
                                }
                                catch (e: Exception) {

                                    controller.clear()


                                    Log.e(
                                        "DriveChronikSESSION",
                                        "SessionInfo rejected: " +
                                            "${e.message}",
                                        e
                                    )
                                }
                            }
                            else {

                                Log.d(
                                    "DriveChronikSESSION",
                                    "SessionInfo ignored: " +
                                        "no pending live session"
                                )
                            }
                        }


                        val encryptedResponse =
                            message.hasSignatureData() &&
                                message
                                    .signatureData
                                    .hasAESGCMResponseData()


                        if (
                            !hasSessionInfo &&
                            !encryptedResponse
                        ) {

                            val payload =
                                message
                                    .protobufMessageAsBytes
                                    .toByteArray()


                            if (payload.isNotEmpty()) {

                                try {

                                    val update =
                                        TeslaVehicleDataRouter
                                            .decode(
                                                payload
                                            )


                                    if (
                                        update.hasVehicleData
                                    ) {

                                        val snapshot =
                                            TeslaDashboardRepository
                                                .apply(
                                                    update
                                                )


                                        Log.d(
                                            "DriveChronikDATA",
                                            "VehicleData " +
                                                "drive=${snapshot.drive != null} " +
                                                "charge=${snapshot.charge != null} " +
                                                "location=${snapshot.location != null}"
                                        )
                                    }
                                }
                                catch (
                                    e:
                                        com.google.protobuf
                                            .InvalidProtocolBufferException
                                ) {

                                    /*
                                     * Nicht jeder Payload aus
                                     * DOMAIN_INFOTAINMENT ist
                                     * CarServer.VehicleData.
                                     */
                                    Log.d(
                                        "DriveChronikDATA",
                                        "RX payload is not CarServer Response"
                                    )
                                }
                            }
                        }
                    }
                    catch (e: Exception) {

                        Log.e(
                            "DriveChronikPROTO",
                            "RoutableMessage decode failed: " +
                                "${e.message}",
                            e
                        )
                    }
                }
            )
        }

    /*
     * Erst NACH vollständig aktivem
     * Tesla-BLE-Transport darf der
     * SessionInfo-Handshake starten.
     *
     * Solange sessionController == null,
     * passiert hier absolut nichts.
     */
    LaunchedEffect(
        gattState.status,
        sessionController
    ) {

        val controller =
            sessionController


        if (
            gattState.status ==
                TeslaGattStatus.READY &&
            controller != null &&
            controller.status() ==
                TeslaProtocolSessionStatus.IDLE
        ) {

            try {

                val handshake =
                    controller
                        .beginHandshake()


                val payload =
                    TeslaRoutableMessageCodec
                        .encode(
                            handshake.message
                        )


                val accepted =
                    gattClient
                        .sendRoutableMessage(
                            payload
                        )


                if (accepted) {

                    Log.d(
                        "DriveChronikSESSION",
                        "SessionInfo TX queued " +
                            "bytes=${payload.size}"
                    )


                    /*
                     * Lokaler Timeout.
                     * Eine erfolgreiche Antwort setzt
                     * den Controller vorher auf READY.
                     */
                    kotlinx.coroutines
                        .delay(
                            11_000L
                        )


                    if (
                        controller.status() ==
                        TeslaProtocolSessionStatus
                            .HANDSHAKE_PENDING
                    ) {

                        controller.clear()


                        Log.w(
                            "DriveChronikSESSION",
                            "SessionInfo timeout"
                        )
                    }
                }
                else {

                    controller.clear()


                    Log.e(
                        "DriveChronikSESSION",
                        "SessionInfo TX rejected by GATT"
                    )
                }
            }
            catch (e: Exception) {

                controller.clear()


                Log.e(
                    "DriveChronikSESSION",
                    "SessionInfo startup failed: " +
                        "${e.message}",
                    e
                )
            }
        }


        if (
            gattState.status ==
                TeslaGattStatus.DISCONNECTED ||
            gattState.status ==
                TeslaGattStatus.ERROR
        ) {

            controller?.clear()
        }
    }


    val permissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) {

            permissionGranted =
                hasBlePermissions(
                    context
                )
        }

    LaunchedEffect(Unit) {

        if (!permissionGranted) {

            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission
                        .BLUETOOTH_SCAN,

                    Manifest.permission
                        .BLUETOOTH_CONNECT
                )
            )
        }
    }

    LaunchedEffect(
        permissionGranted
    ) {

        if (permissionGranted) {

            scanner.startScan(
                durationMs =
                    12_000L
            )
        }
    }

    /*
     * Sobald der Scanner einen Tesla erkennt,
     * beginnt die GATT-Verbindung.
     */
    LaunchedEffect(
        scanState.status,
        scanState.address
    ) {

        if (
            scanState.status ==
                TeslaBleScanStatus.FOUND
            &&
            scanState.address != null
        ) {

            gattClient.connect(
                scanState.address!!
            )
        }
    }

    DisposableEffect(
        scanner,
        gattClient
    ) {

        onDispose {

            scanner.stopScan()
            currentSessionController.value?.clear()
            gattClient.close()
        }
    }

    val text =
        when (gattState.status) {

            TeslaGattStatus.CONNECTING ->
                "Tesla verbindet"

            TeslaGattStatus.CONNECTED ->
                "Tesla verbunden"

            TeslaGattStatus.DISCOVERING_SERVICES ->
                "Tesla Dienste"

            TeslaGattStatus.SERVICE_READY ->
                "Tesla GATT"

            TeslaGattStatus.SUBSCRIBING ->
                "Tesla RX"

            TeslaGattStatus.READY ->
                "Tesla BLE · MTU ${gattState.mtu}"

            TeslaGattStatus.DISCONNECTED ->
                "Tesla getrennt"

            TeslaGattStatus.ERROR ->
                "BLE Fehler"

            TeslaGattStatus.IDLE ->
                when (
                    scanState.status
                ) {

                    TeslaBleScanStatus.IDLE ->
                        "Tesla BLE"

                    TeslaBleScanStatus.SCANNING ->
                        "BLE Scan · ${scanState.devicesSeen}"

                    TeslaBleScanStatus.FOUND ->
                        "Tesla gefunden"

                    TeslaBleScanStatus.NOT_FOUND ->
                        "Tesla nicht gefunden"

                    TeslaBleScanStatus.PERMISSION_MISSING ->
                        "BLE Freigabe"

                    TeslaBleScanStatus.BLUETOOTH_OFF ->
                        "Bluetooth aus"

                    TeslaBleScanStatus.ERROR ->
                        "BLE Fehler"
                }
        }

    val dotColor =
        when {

            gattState.status ==
                TeslaGattStatus.READY ->

                Color(0xFF45D177)


            gattState.status in
                listOf(
                    TeslaGattStatus.CONNECTING,
                    TeslaGattStatus.CONNECTED,
                    TeslaGattStatus.DISCOVERING_SERVICES,
                    TeslaGattStatus.SERVICE_READY,
                    TeslaGattStatus.SUBSCRIBING
                ) ->

                Color(0xFF4A90FF)


            gattState.status ==
                TeslaGattStatus.ERROR ->

                Color(0xFFE82127)


            scanState.status ==
                TeslaBleScanStatus.SCANNING ->

                Color(0xFF4A90FF)


            else ->
                DashMuted
        }

    if (showTeslaSetup) {

        TeslaBleSetupDialog(
            currentConfig =
                liveProtocolConfig,

            keyPresent =
                keyPresent,

            keyFingerprint =
                keyFingerprint,

            keyError =
                keyError,

            onPrepareKey = {

                try {

                    TeslaKeyStore
                        .getOrCreateKeyPair()

                    keyPresent =
                        TeslaKeyStore
                            .hasKey()

                    keyError =
                        null
                }
                catch (e: Exception) {

                    keyPresent =
                        false

                    keyError =
                        e.message
                            ?: "Schlüssel konnte nicht erzeugt werden"
                }
            },

            onDeleteVin = {

                sessionController
                    ?.clear()


                val clearedConfig =
                    liveProtocolConfig.copy(
                        enabled = false,
                        keyEnrollmentConfirmed = false,
                        vin = ""
                    )


                liveProtocolSettings
                    .save(
                        clearedConfig
                    )


                liveProtocolConfig =
                    clearedConfig
            },


            onDeleteKey = {

                sessionController
                    ?.clear()


                /*
                 * Sicherheitszustand zuerst speichern:
                 * Kein Live-BLE und keine behauptete
                 * Tesla-Autorisierung mehr.
                 */
                val safeConfig =
                    liveProtocolConfig.copy(
                        enabled = false,
                        keyEnrollmentConfirmed = false
                    )


                liveProtocolSettings
                    .save(
                        safeConfig
                    )


                liveProtocolConfig =
                    safeConfig


                try {

                    val deleted =
                        TeslaKeyStore
                            .deleteKey()


                    keyPresent =
                        TeslaKeyStore
                            .hasKey()


                    keyError =
                        if (
                            deleted &&
                            !keyPresent
                        )
                            null
                        else
                            "Lokaler Schlüssel konnte nicht gelöscht werden"
                }
                catch (e: Exception) {

                    keyPresent =
                        runCatching {
                            TeslaKeyStore.hasKey()
                        }
                            .getOrDefault(false)


                    keyError =
                        e.message
                            ?: "Lokaler Schlüssel konnte nicht gelöscht werden"
                }
            },


            onDismiss = {

                showTeslaSetup =
                    false
            },

            onSave = { requested ->

                /*
                 * Falls Key/VIN/Bestätigung nicht
                 * vollständig sind, kann Live-BLE
                 * nicht versehentlich aktiv bleiben.
                 */
                val safeConfig =
                    requested.copy(
                        keyEnrollmentConfirmed =
                            requested
                                .keyEnrollmentConfirmed &&
                                keyPresent,

                        enabled =
                            requested.enabled &&
                                requested
                                    .keyEnrollmentConfirmed &&
                                keyPresent &&
                                requested
                                    .normalizedVin
                                    .length == 17
                    )


                /*
                 * Alte Session bewusst beenden,
                 * bevor eine neue Konfiguration
                 * aktiv wird.
                 */
                sessionController
                    ?.clear()


                liveProtocolSettings
                    .save(
                        safeConfig
                    )


                liveProtocolConfig =
                    safeConfig


                showTeslaSetup =
                    false
            }
        )
    }


    Row(
        modifier =
            Modifier
                .clickable {

                    showTeslaSetup =
                        true
                }
                .padding(
                    horizontal = 4.dp,
                    vertical = 2.dp
                ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .background(
                        dotColor,
                        CircleShape
                    )
        )

        Spacer(
            modifier =
                Modifier.width(6.dp)
        )

        Text(
            text = text,
            color = DashMuted,
            fontSize = 11.sp
        )
    }
}


@Composable
private fun Speedometer(
    speed: Float,
    modifier: Modifier = Modifier
) {

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        val gaugeSize =
            minOf(
                maxWidth,
                maxHeight
            )

        Box(
            modifier =
                Modifier.size(gaugeSize),
            contentAlignment =
                Alignment.Center
        ) {

            Canvas(
                modifier =
                    Modifier.fillMaxSize()
            ) {

                val canvasSize =
                    size.minDimension

                val center =
                    Offset(
                        size.width / 2f,
                        size.height / 2f
                    )

                val outerRadius =
                    canvasSize * 0.495f

                /*
                 * dezenter äußerer Ring
                 */
                drawCircle(
                    color =
                        Color.White.copy(
                            alpha = 0.08f
                        ),
                    radius =
                        outerRadius,
                    center =
                        center
                )

                drawCircle(
                    color =
                        DashBackground,
                    radius =
                        outerRadius - 2.5f,
                    center =
                        center
                )

                drawCircle(
                    color =
                        Color.White.copy(
                            alpha = 0.07f
                        ),
                    radius =
                        outerRadius * 0.93f,
                    center =
                        center
                )

                drawCircle(
                    color =
                        Color(0xFF0A0B0D),
                    radius =
                        outerRadius * 0.92f,
                    center =
                        center
                )

                /*
                 * 0 bis 200 km/h.
                 * Teilstrich alle 5 km/h.
                 */
                for (value in 0..200 step 5) {

                    val angle =
                        135f +
                        value / 200f * 270f

                    val rad =
                        angle * PI.toFloat() /
                        180f

                    val isMajor =
                        value % 25 == 0

                    val isMedium =
                        !isMajor &&
                        value % 10 == 0

                    val outer =
                        outerRadius * 0.87f

                    val length =
                        when {
                            isMajor ->
                                outerRadius * 0.060f

                            isMedium ->
                                outerRadius * 0.039f

                            else ->
                                outerRadius * 0.024f
                        }

                    val startRadius =
                        outer - length

                    val start =
                        Offset(
                            x =
                                center.x +
                                cos(rad) *
                                startRadius,

                            y =
                                center.y +
                                sin(rad) *
                                startRadius
                        )

                    val end =
                        Offset(
                            x =
                                center.x +
                                cos(rad) *
                                outer,

                            y =
                                center.y +
                                sin(rad) *
                                outer
                        )

                    drawLine(
                        color =
                            when {
                                isMajor ->
                                    Color.White.copy(
                                        alpha = 0.88f
                                    )

                                isMedium ->
                                    Color.White.copy(
                                        alpha = 0.52f
                                    )

                                else ->
                                    Color.White.copy(
                                        alpha = 0.28f
                                    )
                            },

                        start = start,
                        end = end,

                        strokeWidth =
                            when {
                                isMajor -> 3.0f
                                isMedium -> 2.0f
                                else -> 1.2f
                            },

                        cap =
                            StrokeCap.Round
                    )
                }

                /*
                 * Beschriftung:
                 * 0 / 50 / 100 / 150 / 200
                 */
                val paint =
                    Paint().apply {

                        color =
                            android.graphics.Color
                                .argb(
                                    190,
                                    225,
                                    229,
                                    233
                                )

                        textAlign =
                            Paint.Align.CENTER

                        textSize =
                            canvasSize * 0.053f

                        typeface =
                            Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.BOLD
                            )

                        isAntiAlias = true
                    }

                for (
                    value in
                    listOf(
                        0,
                        50,
                        100,
                        150,
                        200
                    )
                ) {

                    val angle =
                        135f +
                        value / 200f * 270f

                    val rad =
                        angle *
                        PI.toFloat() /
                        180f

                    val labelRadius =
                        outerRadius * 0.70f

                    val x =
                        center.x +
                        cos(rad) *
                        labelRadius

                    val y =
                        center.y +
                        sin(rad) *
                        labelRadius +
                        paint.textSize * 0.35f

                    drawContext
                        .canvas
                        .nativeCanvas
                        .drawText(
                            value.toString(),
                            x,
                            y,
                            paint
                        )
                }

                /*
                 * rote Tachonadel
                 */
                val shownSpeed =
                    speed.coerceIn(
                        0f,
                        200f
                    )

                val needleAngle =
                    135f +
                    shownSpeed /
                    200f *
                    270f

                val needleRad =
                    needleAngle *
                    PI.toFloat() /
                    180f

                val needleLength =
                    outerRadius * 0.75f

                val needleEnd =
                    Offset(
                        x =
                            center.x +
                            cos(needleRad) *
                            needleLength,

                        y =
                            center.y +
                            sin(needleRad) *
                            needleLength
                    )

                val needleTail =
                    Offset(
                        x =
                            center.x -
                            cos(needleRad) *
                            outerRadius *
                            0.11f,

                        y =
                            center.y -
                            sin(needleRad) *
                            outerRadius *
                            0.11f
                    )

                /*
                 * leichter roter Glow
                 */
                drawLine(
                    color =
                        DashRed.copy(
                            alpha = 0.22f
                        ),
                    start =
                        needleTail,
                    end =
                        needleEnd,
                    strokeWidth = 9f,
                    cap =
                        StrokeCap.Round
                )

                drawLine(
                    color =
                        Color(0xFFE82127),
                    start =
                        needleTail,
                    end =
                        needleEnd,
                    strokeWidth = 4f,
                    cap =
                        StrokeCap.Round
                )

                drawCircle(
                    color =
                        Color(0xFF151719),
                    radius =
                        outerRadius *
                        0.042f,
                    center =
                        center
                )

                drawCircle(
                    color =
                        DashRed,
                    radius =
                        outerRadius *
                        0.027f,
                    center =
                        center
                )
            }

            /*
             * digitale Geschwindigkeit
             */
            Column(
                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
                        .offset(
                            y =
                                gaugeSize *
                                0.18f
                        ),
                horizontalAlignment =
                    Alignment.CenterHorizontally
            ) {

                Text(
                    text =
                        speed
                            .roundToInt()
                            .toString(),
                    color =
                        DashText,
                    fontSize =
                        70.sp,
                    fontWeight =
                        FontWeight.Light
                )

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text = "km/h",
                        color = DashMuted,
                        fontSize = 13.sp
                    )

                    Spacer(
                        modifier =
                            Modifier.width(9.dp)
                    )

                    Text(
                        text = "D",
                        color = DashText,
                        fontSize = 18.sp,
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun SpeedLimitSign(
    limit: Int,
    modifier: Modifier = Modifier
) {

    Box(
        modifier =
            modifier
                .size(90.dp)
                .background(
                    Color.White,
                    CircleShape
                )
                .border(
                    width = 8.dp,
                    color =
                        Color(0xFFE52626),
                    shape =
                        CircleShape
                ),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text =
                limit.toString(),
            color =
                Color.Black,
            fontSize =
                38.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}

@Composable
private fun RestEnergyPanel(
    remainingKwh: Float,
    maxKwh: Float,
    compact: Boolean = false
) {

    val percentage =
        if (maxKwh > 0f)
            (
                remainingKwh /
                maxKwh
            ).coerceIn(
                0f,
                1f
            )
        else
            0f

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(
                    if (compact)
                        92.dp
                    else
                        104.dp
                )
                .border(
                    width = 0.dp,
                    color = Color.Transparent
                )
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 7.dp,
                    bottom = 4.dp
                )
    ) {

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashLine)
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            verticalAlignment =
                Alignment.Bottom
        ) {

            Text(
                text = "RESTENERGIE",
                color = DashMuted,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )

            Spacer(
                modifier =
                    Modifier.weight(1f)
            )

            Text(
                text =
                    String.format(
                        "%.1f",
                        remainingKwh
                    ),
                color = DashText,
                fontSize = 36.sp,
                fontWeight =
                    FontWeight.SemiBold
            )

            Spacer(
                modifier =
                    Modifier.width(5.dp)
            )

            Text(
                text = "kWh",
                color = DashMuted,
                fontSize = 12.sp,
                modifier =
                    Modifier.padding(
                        bottom = 2.dp
                    )
            )
        }

        Spacer(
            modifier =
                Modifier.height(7.dp)
        )

        EnergyBar(
            percentage =
                percentage
        )

        Spacer(
            modifier =
                Modifier.height(4.dp)
        )

        Row(
            modifier =
                Modifier.fillMaxWidth(),
            horizontalArrangement =
                Arrangement.SpaceBetween
        ) {

            listOf(
                "0%",
                "25%",
                "50%",
                "75%",
                "100%"
            ).forEach {

                Text(
                    text = it,
                    color = DashMuted,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
private fun EnergyBar(
    percentage: Float
) {

    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(
                    RoundedCornerShape(
                        20.dp
                    )
                )
    ) {

        /*
         * fließend:
         * Rot -> Violett -> Blau ->
         * Türkis -> Grün
         */
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFEE3030),
                                Color(0xFFC23B73),
                                Color(0xFF6B58C7),
                                Color(0xFF3478F6),
                                Color(0xFF319BC1),
                                Color(0xFF2AAC78),
                                Color(0xFF28B45B)
                            )
                        )
                    )
        )

        /*
         * nicht verfügbare Restfläche abdunkeln
         */
        Box(
            modifier =
                Modifier
                    .align(
                        Alignment.CenterEnd
                    )
                    .fillMaxHeight()
                    .fillMaxWidth(
                        1f - percentage
                    )
                    .background(
                        Color(0xD9030405)
                    )
        )

        /*
         * weißer Marker
         */
        Box(
            modifier =
                Modifier
                    .offset(
                        x =
                            maxWidth *
                            percentage -
                            1.dp
                    )
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(
                        Color.White
                    )
        )
    }
}


@Composable
private fun VehicleStatusBar(
    leftIndicator: Boolean,
    rightIndicator: Boolean,
    lowBeam: Boolean,
    highBeam: Boolean,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {

        /*
         * Nur eine feine obere Trennlinie.
         * Keine Karte, kein Rahmen.
         */
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DashLine)
        )

        Row(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = 14.dp
                    ),
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            StatusIndicator(
                symbol = "◀",
                active = leftIndicator,
                activeColor =
                    Color(0xFF45D177),
                modifier =
                    Modifier.weight(1f)
            )

            StatusIndicator(
                symbol = "◐",
                active = lowBeam,
                activeColor =
                    Color(0xFF55C878),
                modifier =
                    Modifier.weight(1f)
            )

            StatusIndicator(
                symbol = "✦",
                active = highBeam,
                activeColor =
                    Color(0xFF4A90FF),
                modifier =
                    Modifier.weight(1f)
            )

            StatusIndicator(
                symbol = "▶",
                active = rightIndicator,
                activeColor =
                    Color(0xFF45D177),
                modifier =
                    Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun StatusIndicator(
    symbol: String,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {

    val inactiveColor =
        Color(0xFF303338)

    Box(
        modifier =
            modifier.fillMaxHeight(),
        contentAlignment =
            Alignment.Center
    ) {

        Text(
            text = symbol,
            color =
                if (active)
                    activeColor
                else
                    inactiveColor,
            fontSize = 38.sp,
            fontWeight =
                FontWeight.Bold
        )
    }
}


@Composable
private fun KpiCard(
    title: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier,
    valueColor: Color = DashText
) {

    Column(
        modifier =
            modifier
                .padding(
                    horizontal = 18.dp,
                    vertical = 12.dp
                ),
        verticalArrangement =
            Arrangement.Center
    ) {

        Text(
            text = title,
            color = DashMuted,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.5.sp,
            maxLines = 1
        )

        Spacer(
            modifier =
                Modifier.height(8.dp)
        )

        Row(
            verticalAlignment =
                Alignment.Bottom
        ) {

            Text(
                text = value,
                color = valueColor,
                fontSize = 40.sp,
                fontWeight =
                    FontWeight.Normal
            )

            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )

            Text(
                text = unit,
                color = DashMuted,
                fontSize = 18.sp,
                fontWeight =
                    FontWeight.Medium,
                modifier =
                    Modifier.padding(
                        bottom = 4.dp
                    )
            )
        }
    }
}


@Preview(
    widthDp = 915,
    heightDp = 412,
    showBackground = true,
    backgroundColor = 0xFF030405
)
@Composable
private fun DashboardPreview() {
    DriveChronikDashTheme {
        DashboardContent(
            state =
                DashboardUiState(
                    speed = 100f,
                    speedLimit = 100,
                    powerKw = 15.3f,
                    remainingKwh = 43.7f,
                    usableCapacityKwh = 60f,
                    rangeKm = 308
                )
        )
    }
}



private fun teslaKeyFingerprint():
    String? {

    return try {

        if (
            !TeslaKeyStore.hasKey()
        ) {
            return null
        }


        val publicKey =
            TeslaKeyStore
                .encodedPublicKey()


        val digest =
            java.security
                .MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    publicKey
                )


        digest.joinToString(
            ":"
        ) {

            "%02X".format(
                it.toInt() and 0xff
            )
        }
    }
    catch (_: Exception) {

        null
    }
}


@Composable
private fun TeslaBleSetupDialog(

    currentConfig:
        TeslaLiveProtocolConfig,

    keyPresent:
        Boolean,

    keyFingerprint:
        String?,

    keyError:
        String?,

    onPrepareKey:
        () -> Unit,

    onDeleteVin:
        () -> Unit,

    onDeleteKey:
        () -> Unit,

    onDismiss:
        () -> Unit,

    onSave:
        (TeslaLiveProtocolConfig) -> Unit

) {

    var vin by
        remember(currentConfig) {
            mutableStateOf(
                currentConfig.vin
            )
        }


    var enrollmentConfirmed by
        remember(currentConfig) {
            mutableStateOf(
                currentConfig
                    .keyEnrollmentConfirmed
            )
        }


    var liveEnabled by
        remember(currentConfig) {
            mutableStateOf(
                currentConfig.enabled
            )
        }


    var confirmVinDeletion by
        remember {
            mutableStateOf(false)
        }


    var confirmKeyDeletion by
        remember {
            mutableStateOf(false)
        }


    val normalizedVin =
        vin
            .trim()
            .uppercase(
                java.util.Locale.ROOT
            )


    val vinValid =
        normalizedVin.length == 17


    val vinSaved =
        vinValid &&
            normalizedVin ==
            currentConfig.normalizedVin


    val livePossible =
        keyPresent &&
            enrollmentConfirmed &&
            vinValid


    val statusText =
        when {

            !keyPresent ->
                "Lokaler Schlüssel fehlt"

            !vinValid ->
                "VIN unvollständig"

            !enrollmentConfirmed ->
                "Schlüssel noch nicht autorisiert"

            liveEnabled ->
                "Live-BLE aktiviert"

            else ->
                "Bereit, aber deaktiviert"
        }


    AlertDialog(

        modifier =
            Modifier.fillMaxWidth(0.90f),

        properties =
            DialogProperties(
                usePlatformDefaultWidth =
                    false
            ),

        onDismissRequest =
            onDismiss,

        containerColor =
            Color(0xFF3A3F46),

        titleContentColor =
            Color.White,

        textContentColor =
            Color(0xFFF0F1F3),

        title = {

            Text(
                text =
                    "Tesla BLE Setup",

                color =
                    Color.White,

                fontSize =
                    22.sp,

                fontWeight =
                    FontWeight.Bold
            )
        },

        text = {

            Row(

                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(
                        30.dp
                    )
            ) {

                /*
                 * =====================================
                 * LINKE SPALTE
                 * VIN + lokaler Schlüssel
                 * =====================================
                 */
                Column(

                    modifier =
                        Modifier.weight(1.25f),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            4.dp
                        )
                ) {

                    Text(
                        text =
                            "Alles bleibt lokal auf diesem Gerät.",

                        color =
                            Color(0xFFE3E5E8),

                        fontSize =
                            11.sp
                    )


                    OutlinedTextField(

                        value =
                            vin,

                        onValueChange = { value ->

                            vin =
                                value
                                    .filter {
                                        it.isLetterOrDigit()
                                    }
                                    .take(17)
                                    .uppercase(
                                        java.util.Locale.ROOT
                                    )

                            confirmVinDeletion =
                                false
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        singleLine =
                            true,

                        label = {

                            Text(
                                "VIN"
                            )
                        },

                        supportingText = {

                            Text(
                                text =
                                    when {

                                        vinSaved ->
                                            "VIN gespeichert"

                                        vinValid ->
                                            "VIN vollständig – noch nicht gespeichert"

                                        else ->
                                            "${normalizedVin.length}/17 Zeichen"
                                    },

                                color =
                                    if (vinSaved)
                                        Color(0xFF45D177)
                                    else
                                        Color(0xFFE0E2E5),

                                fontSize =
                                    10.sp
                            )
                        },

                        colors =
                            OutlinedTextFieldDefaults
                                .colors(

                                    focusedTextColor =
                                        Color.White,

                                    unfocusedTextColor =
                                        Color.White,

                                    cursorColor =
                                        DashRed,

                                    focusedBorderColor =
                                        Color(0xFFB2B7BE),

                                    unfocusedBorderColor =
                                        Color(0xFF858B94),

                                    focusedLabelColor =
                                        Color.White,

                                    unfocusedLabelColor =
                                        Color(0xFFE0E2E5),

                                    focusedContainerColor =
                                        Color(0xFF4A5058),

                                    unfocusedContainerColor =
                                        Color(0xFF4A5058)
                                )
                    )


                    if (
                        currentConfig
                            .normalizedVin
                            .isNotEmpty()
                    ) {

                        TextButton(

                            onClick = {

                                if (
                                    confirmVinDeletion
                                ) {

                                    onDeleteVin()

                                    vin =
                                        ""

                                    enrollmentConfirmed =
                                        false

                                    liveEnabled =
                                        false

                                    confirmVinDeletion =
                                        false
                                }
                                else {

                                    confirmVinDeletion =
                                        true
                                }
                            }
                        ) {

                            Text(
                                text =
                                    if (
                                        confirmVinDeletion
                                    )
                                        "VIN wirklich löschen"
                                    else
                                        "VIN löschen",

                                color =
                                    Color(0xFFFFB4AB),

                                fontSize =
                                    11.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    if (keyPresent) {
                        Row(

                            modifier =
                                Modifier.fillMaxWidth(),

                            horizontalArrangement =
                                Arrangement.SpaceBetween,

                            verticalAlignment =
                                Alignment.CenterVertically
                        ) {

                            Text(
                                text =
                                    "Lokaler Schlüssel vorhanden",

                                color =
                                    Color(0xFF45D177),

                                fontSize =
                                    12.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )


                            TextButton(

                                onClick = {

                                    if (
                                        confirmKeyDeletion
                                    ) {

                                        onDeleteKey()

                                        enrollmentConfirmed =
                                            false

                                        liveEnabled =
                                            false

                                        confirmKeyDeletion =
                                            false
                                    }
                                    else {

                                        confirmKeyDeletion =
                                            true
                                    }
                                }
                            ) {

                                Text(
                                    text =
                                        if (
                                            confirmKeyDeletion
                                        )
                                            "Löschen bestätigen"
                                        else
                                            "Schlüssel löschen",

                                    color =
                                        Color(0xFFFF6B6B),

                                    fontSize =
                                        10.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )
                            }
                        }


                        Text(
                            text =
                                if (keyFingerprint != null) {

                                    val fp =
                                        keyFingerprint

                                    val shortFp =
                                        if (fp.length > 40)
                                            fp.take(20) +
                                                " … " +
                                                fp.takeLast(20)
                                        else
                                            fp

                                    "SHA-256: $shortFp"
                                }
                                else {

                                    "SHA-256: nicht verfügbar"
                                },

                            color =
                                Color(0xFFE6E8EB),

                            fontSize =
                                9.sp,

                            maxLines =
                                1
                        )
}
                    else {

                        Text(
                            text =
                                "Kein lokaler Tesla-Schlüssel vorhanden.",

                            color =
                                Color.White,

                            fontSize =
                                11.sp
                        )


                        TextButton(
                            onClick =
                                onPrepareKey
                        ) {

                            Text(
                                text =
                                    "Lokalen Schlüssel erzeugen",

                                color =
                                    Color.White,

                                fontSize =
                                    11.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }
                    }


                    if (keyError != null) {

                        Text(
                            text =
                                keyError,

                            color =
                                DashRed,

                            fontSize =
                                10.sp
                        )
                    }
                }


                /*
                 * =====================================
                 * RECHTE SPALTE
                 * Freigaben + Status
                 * =====================================
                 */
                Column(

                    modifier =
                        Modifier.weight(0.85f),

                    verticalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        )
                ) {

                    Text(
                        text =
                            "Freigaben",

                        color =
                            Color.White,

                        fontSize =
                            15.sp,

                        fontWeight =
                            FontWeight.Bold
                    )


                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Checkbox(

                            checked =
                                enrollmentConfirmed,

                            enabled =
                                keyPresent,

                            colors =
                                CheckboxDefaults
                                    .colors(

                                        checkedColor =
                                            DashRed,

                                        uncheckedColor =
                                            Color.White,

                                        checkmarkColor =
                                            Color.White,

                                        disabledUncheckedColor =
                                            Color(0xFF8F949C)
                                    ),

                            onCheckedChange = { checked ->

                                enrollmentConfirmed =
                                    checked

                                if (!checked) {

                                    liveEnabled =
                                        false
                                }
                            }
                        )


                        Text(
                            text =
                                "Schlüssel von Tesla autorisieren lassen",

                            modifier =
                                Modifier.weight(1f),

                            color =
                                Color.White,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Medium
                        )
                    }


                    Row(

                        modifier =
                            Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Checkbox(

                            checked =
                                liveEnabled,

                            enabled =
                                livePossible,

                            colors =
                                CheckboxDefaults
                                    .colors(

                                        checkedColor =
                                            DashRed,

                                        uncheckedColor =
                                            Color.White,

                                        checkmarkColor =
                                            Color.White,

                                        disabledUncheckedColor =
                                            Color(0xFF8F949C)
                                    ),

                            onCheckedChange = { checked ->

                                liveEnabled =
                                    checked
                            }
                        )


                        Text(
                            text =
                                "Tesla-Livedaten über Bluetooth aktivieren",

                            modifier =
                                Modifier.weight(1f),

                            color =
                                Color.White,

                            fontSize =
                                12.sp,

                            fontWeight =
                                FontWeight.Bold
                        )
                    }


                    Text(
                        text =
                            "Status: $statusText",

                        color =
                            if (
                                liveEnabled &&
                                livePossible
                            )
                                Color(0xFF45D177)
                            else
                                Color.White,

                        fontSize =
                            12.sp,

                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        },

        confirmButton = {

            Button(

                onClick = {

                    onSave(
                        TeslaLiveProtocolConfig(

                            enabled =
                                liveEnabled,

                            keyEnrollmentConfirmed =
                                enrollmentConfirmed,

                            vin =
                                normalizedVin
                        )
                    )
                },

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            Color(0xFF45D177),

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "Speichern",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },

        dismissButton = {

            Button(

                onClick =
                    onDismiss,

                colors =
                    ButtonDefaults.buttonColors(
                        containerColor =
                            DashRed,

                        contentColor =
                            Color.White
                    )
            ) {

                Text(
                    text =
                        "Abbrechen",

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    )
}
