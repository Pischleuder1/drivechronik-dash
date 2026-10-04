package de.drivechronik.dash

import androidx.compose.animation.core.tween

import androidx.compose.animation.core.animateFloatAsState

import de.drivechronik.dash.diagnostics.TelemetryDiagnosticsLogger
import de.drivechronik.dash.diagnostics.DiagnosticsSettings

import android.graphics.Paint
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import de.drivechronik.dash.ble.TeslaBleScanState
import de.drivechronik.dash.ble.TeslaBleScanStatus
import de.drivechronik.dash.ble.TeslaBleScanner
import de.drivechronik.dash.ble.TeslaBleVehicleIdentity
import de.drivechronik.dash.ble.TeslaBleGattClient
import de.drivechronik.dash.ble.TeslaRoutableMessageCodec
import de.drivechronik.dash.ble.TeslaGattState
import de.drivechronik.dash.ble.TeslaGattStatus
import de.drivechronik.dash.ble.TeslaBleSessionController
import de.drivechronik.dash.ble.TeslaKeyStore
import de.drivechronik.dash.ble.TeslaKeyEnrollmentRequest
import de.drivechronik.dash.ble.TeslaLiveProtocolSettings
import de.drivechronik.dash.ble.TeslaLiveProtocolConfig
import de.drivechronik.dash.ble.TeslaProtocolSessionStatus
import com.tesla.generated.universalmessage.UniversalMessage
import de.drivechronik.dash.ble.TeslaVehicleDataRouter
import de.drivechronik.dash.ble.TeslaNavigationTelemetry
import de.drivechronik.dash.ble.TeslaDashboardTelemetryMapper
import de.drivechronik.dash.data.TeslaDashboardRepository
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import de.drivechronik.dash.theme.DriveChronikDashTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
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


/*
 * Nachtfarben des Tachometers.
 * Layout und Statusfarben bleiben unverändert.
 */
private val DashNightRing = Color(0xFF174A78)
private val DashNightTickMajor = Color(0xFF4F9DFF)
private val DashNightTickMedium = Color(0xFF3275B5)
private val DashNightTickMinor = Color(0xFF20537F)


data class DashboardUiState(
    val speed: Float = 100f,
    val speedLimit: Int = 100,
    val powerKw: Float = 15.3f,
    val remainingKwh: Float = 43.7f,
    val usableCapacityKwh: Float = 60f,
    val rangeKm: Int = 308,
        val gear: String = "D",
    val navigation: TeslaNavigationTelemetry? = null,
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
                    DriveChronikDashboard(
                        navigationPreview =
                            intent.getBooleanExtra(
                                "navigationPreview",
                                false
                            ),
                        demoPreview =
                            intent.getBooleanExtra(
                                "demoPreview",
                                false
                            ),
                        telemetryDiagnostics =
                            intent.getBooleanExtra(
                                "telemetryDiagnostics",
                                false
                            )
                    )
                }
            }
        }
    }


    override fun onResume() {
        super.onResume()
        applyImmersiveMode()
    }


    override fun onWindowFocusChanged(
        hasFocus: Boolean
    ) {
        super.onWindowFocusChanged(
            hasFocus
        )

        if (hasFocus) {
            applyImmersiveMode()
        }
    }


    private fun applyImmersiveMode() {

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
                    WindowInsetsCompat
                        .Type
                        .systemBars()
                )

                systemBarsBehavior =
                    WindowInsetsControllerCompat
                        .BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
    }
}

@Composable
private fun DriveChronikDashboard(
    navigationPreview: Boolean = false,
    demoPreview: Boolean = false,
    telemetryDiagnostics: Boolean = false
) {

    /*
     * LIVE ist der Standard.
     *
     * demoPreview bleibt für ADB/UI-Tests erhalten.
     * Über das Service-Menü kann Demo zusätzlich
     * zur Laufzeit ein- und ausgeschaltet werden.
     */
    var demoEnabled by
        remember(
            demoPreview
        ) {
            mutableStateOf(
                demoPreview
            )
        }


    /*
     * Demo-Werte existieren weiterhin ausschließlich
     * für die explizit gestartete UI-Vorschau.
     *
     * Im normalen Fahrzeugbetrieb werden sie niemals
     * als Fallback verwendet.
     */
    var demoSpeed by remember {
        mutableFloatStateOf(96f)
    }

    var demoPowerKw by remember {
        mutableFloatStateOf(15.3f)
    }

    var demoRemainingKwh by remember {
        mutableFloatStateOf(43.8f)
    }

    var demoSpeedLimit by remember {
        mutableIntStateOf(100)
    }

    var demoTick by remember {
        mutableIntStateOf(0)
    }


    /*
     * Reale Tesla-Daten.
     */
    val liveSnapshot by
        TeslaDashboardRepository
            .snapshot
            .collectAsState()


    /*
     * Die Datenalterung muss auch dann weiterlaufen,
     * wenn gerade kein neues BLE-Paket eintrifft.
     */
    var telemetryNowNanos by
        remember {
            mutableStateOf(
                System.nanoTime()
            )
        }


    LaunchedEffect(Unit) {

        while (true) {

            telemetryNowNanos =
                System.nanoTime()

            delay(
                500L
            )
        }
    }


    val liveTelemetry =
        TeslaDashboardTelemetryMapper
            .map(
                snapshot =
                    liveSnapshot,

                nowNanos =
                    telemetryNowNanos
            )


    /*
     * Lokales Diagnoseprotokoll.
     *
     * Nur aktiv, wenn die App ausdrücklich mit
     * telemetryDiagnostics=true gestartet wurde.
     *
     * Keine Cloud.
     * Kein Netzwerk.
     * Keine VIN.
     * Keine Schlüssel-/Auth-Daten.
     */
    /*
     * Diagnosemodus kann entweder über den bisherigen
     * Intent-Parameter oder direkt im Dashboard aktiviert
     * werden.
     *
     * Der Zustand aus dem Service-Menü wird lokal gespeichert
     * und beim nächsten App-Start wiederhergestellt.
     */
    val diagnosticsContext =
        LocalContext
            .current
            .applicationContext


    val diagnosticsSettings =
        remember(
            diagnosticsContext
        ) {
            DiagnosticsSettings(
                diagnosticsContext
            )
        }


    var diagnosticsEnabled by
        remember(
            telemetryDiagnostics,
            diagnosticsSettings
        ) {
            mutableStateOf(
                telemetryDiagnostics ||
                    diagnosticsSettings.load()
            )
        }


    val diagnosticsLogger =
        remember(
            diagnosticsEnabled,
            diagnosticsContext
        ) {

            if (
                diagnosticsEnabled
            ) {

                TelemetryDiagnosticsLogger(
                    diagnosticsContext
                )
            }
            else {
                null
            }
        }


    LaunchedEffect(
        diagnosticsEnabled,
        diagnosticsLogger
    ) {

        if (
            !diagnosticsEnabled
        ) {
            return@LaunchedEffect
        }


        val logger =
            diagnosticsLogger
                ?: return@LaunchedEffect


        while (true) {

            logger.log(
                snapshot =
                    TeslaDashboardRepository
                        .snapshot
                        .value,

                nowNanos =
                    System.nanoTime()
            )


            delay(
                1_000L
            )
        }
    }


    val driveFresh =
        TeslaDashboardTelemetryMapper
            .isDriveFresh(
                liveTelemetry
            )


    /*
     * Tesla dokumentiert drive_state.speed als mph.
     *
     * Deshalb verwenden wir bewusst das Integer-Feld
     * "speed" aus TeslaDriveStateData und NICHT
     * speedFloat.
     *
     * mph -> km/h:
     * 1 mph = 1.609344 km/h
     */
    val liveSpeedKmh =
        if (driveFresh) {

            liveSnapshot
                .drive
                ?.speed
                ?.takeIf {
                    it >= 0L
                }
                ?.toFloat()
                ?.times(
                    1.609344f
                )
        }
        else {
            null
        }


    /*
     * Gang:
     *
     * Demo nur in demoEnabled.
     *
     * Im Normalbetrieb:
     * frisch -> echter Tesla-Wert
     * alt / unbekannt -> —
     *
     * Niemals zurück auf einen Demo-Gang.
     */
    val displayGear =
        if (demoEnabled) {

            "D"
        }
        else {

            TeslaDashboardTelemetryMapper
                .displayGear(
                    liveTelemetry
                )
                ?: "—"
        }


    /*
     * Demo-Simulation läuft NUR noch dann,
     * wenn demoEnabled=true gestartet wurde.
     */
    LaunchedEffect(
        demoEnabled
    ) {

        if (!demoEnabled) {
            return@LaunchedEffect
        }


        val limits =
            listOf(
                50,
                70,
                100,
                120
            )

        var limitIndex =
            2


        while (true) {

            delay(
                400L
            )

            demoTick++


            demoSpeed =
                96f +
                    sin(
                        demoTick *
                            0.055f
                    ) *
                    11f


            demoPowerKw =
                10f +
                    sin(
                        demoTick *
                            0.10f
                    ) *
                    18f


            demoRemainingKwh =
                (
                    demoRemainingKwh -
                        0.0008f
                    )
                    .coerceAtLeast(
                        0f
                    )


            if (
                demoTick %
                    100 ==
                    0
            ) {

                limitIndex =
                    (
                        limitIndex +
                            1
                        ) %
                        limits.size

                demoSpeedLimit =
                    limits[
                        limitIndex
                    ]
            }
        }
    }


    val demoRangeKm =
        (
            demoRemainingKwh *
                1000f /
                142f
            )
            .roundToInt()


    /*
     * Navigation:
     *
     * navigationPreview ist ausschließlich
     * die gezielte UI-Vorschau.
     *
     * Ansonsten werden nur echte und frische
     * Tesla-Routendaten angezeigt.
     */
    val activeNavigation =
        if (navigationPreview && demoEnabled) {

            TeslaNavigationTelemetry(
                destination =
                    "Hannover",

                remainingDistanceKm =
                    42f,

                remainingMinutes =
                    31f,

                trafficDelayMinutes =
                    4f,

                energyAtArrivalPercent =
                    54f
            )
        }
        else {

            TeslaDashboardTelemetryMapper
                .activeNavigation(
                    liveTelemetry
                )
        }


    /*
     * ========================================================
     * SICHTBARE DASHBOARD-WERTE
     * ========================================================
     *
     * Normalbetrieb:
     *
     * Geschwindigkeit:
     *   echte Tesla-Daten oder 0
     *
     * Gang:
     *   echter Tesla-Wert oder —
     *
     * Navigation:
     *   echte Tesla-Route oder ausgeblendet
     *
     * Leistung / Reichweite / Restenergie /
     * Tempolimit / Blinker / Licht:
     *
     *   bleiben neutral, solange deren Live-Daten
     *   noch nicht verifiziert sind.
     *
     * Entscheidend:
     * Es gibt KEINEN automatischen Rückfall
     * auf Demodaten.
     */
    Box(
        modifier =
            Modifier.fillMaxSize()
    ) {

        DashboardContent(
            state =
                DashboardUiState(

                    speed =
                        if (demoEnabled) {
                            demoSpeed
                        }
                        else {
                            liveSpeedKmh
                                ?: 0f
                        },


                    /*
                     * 0 bedeutet:
                     * keine verifizierte Tempolimitquelle.
                     *
                     * SpeedLimitSign blendet sich bei <= 0 aus.
                     */
                    speedLimit =
                        if (demoEnabled) {
                            demoSpeedLimit
                        }
                        else {
                            0
                        },


                    /*
                     * power ist im lokalen Proto vorhanden,
                     * aber Einheit / Vorzeichenlogik wird
                     * erst am realen Fahrzeug verifiziert.
                     */
                    powerKw =
                        if (demoEnabled) {
                            demoPowerKw
                        }
                        else {
                            0f
                        },


                    remainingKwh =
                        if (demoEnabled) {
                            demoRemainingKwh
                        }
                        else {
                            0f
                        },


                    usableCapacityKwh =
                        60f,


                    rangeKm =
                        if (demoEnabled) {
                            demoRangeKm
                        }
                        else {
                            0
                        },


                    gear =
                        displayGear,


                    navigation =
                if (demoEnabled) {
                    TeslaNavigationTelemetry(
                        destination = "Hannover",
                        remainingDistanceKm = 42f,
                        remainingMinutes = 31f,
                        trafficDelayMinutes = 4f,
                        energyAtArrivalPercent = 54f
                    )
                } else {
                    activeNavigation
                },


                    /*
                     * Blinker/Licht bleiben im Normalbetrieb
                     * bewusst inaktiv, bis die Live-Signale
                     * verifiziert und angeschlossen sind.
                     */
                    leftIndicator =
                        demoEnabled &&
                            (
                                demoTick %
                                    120 in
                                    10..34
                                ) &&
                            (
                                demoTick %
                                    4 <
                                    2
                                ),


                    rightIndicator =
                        demoEnabled &&
                            (
                                demoTick %
                                    120 in
                                    72..96
                                ) &&
                            (
                                demoTick %
                                    4 <
                                    2
                                ),


                    lowBeam =
                        demoEnabled &&
                            (
                                demoTick %
                                    120 !in
                                    48..62
                                ),


                    highBeam =
                        demoEnabled &&
                            (
                                demoTick %
                                    120 in
                                    48..62
                                )
                )
        )

        DiagnosticsControlOverlay(
            demoEnabled =
                demoEnabled,

            onDemoEnabledChange = {
                demoEnabled =
                    it
            },

            diagnosticsEnabled =
                diagnosticsEnabled,

            onDiagnosticsEnabledChange = { enabled ->

                diagnosticsSettings.save(
                    enabled
                )

                diagnosticsEnabled =
                    enabled
            },

            modifier =
                Modifier
                    .align(
                        Alignment.TopStart
                    )
                    .padding(
                        start = 8.dp,
                        top = 5.dp
                    )
        )
    }

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
                        speedLimit = state.speedLimit,
                        remainingKwh =
                            state.remainingKwh,

                        usableCapacityKwh =
                            state.usableCapacityKwh,

                                                gear = state.gear,
modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    top = 2.dp,
                                    end = 30.dp,
                                    bottom = 2.dp
                                )
                                .offset(
                                    x = (-38).dp
                                )
                    )


                    /*
                     * Restenergie als äußerer Tachokranz.
                     *
                     * 0 %   = Position 0 km/h
                     * 100 % = Position 100 km/h
                     */
                    EnergyRing(
                        remainingKwh =
                            state.remainingKwh,

                        maxKwh =
                            state.usableCapacityKwh,

                        modifier =
                            Modifier
                                .fillMaxSize()
                                .padding(
                                    top = 2.dp,
                                    end = 30.dp,
                                    bottom = 2.dp
                                )
                                .offset(
                                    x = (-38).dp
                                )
                    )


                    PowerRing(
                        powerKw = state.powerKw,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(
                                top = 2.dp,
                                end = 30.dp,
                                bottom = 2.dp
                            )
                            .offset(x = (-38).dp)
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



                state.navigation?.let {
                    navigation ->

                    NavigationPanel(
                        navigation =
                            navigation,

                        modifier =
                            Modifier
                                .fillMaxWidth()
                    )
                }


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
private fun DiagnosticsControlOverlay(
    demoEnabled: Boolean,
    onDemoEnabledChange: (Boolean) -> Unit,
    diagnosticsEnabled: Boolean,
    onDiagnosticsEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {

    var showDialog by
        remember {
            mutableStateOf(
                false
            )
        }


    /*
     * Unsichtbare Fläche über dem Titel.
     * 5 Sekunden gedrückt halten öffnet das Service-Menü.
     */
    Box(
        modifier =
            modifier
                .width(300.dp)
                .height(30.dp)
                .pointerInput(Unit) {

                    detectTapGestures(

                        onPress = {

                            val releasedBeforeTimeout =
                                withTimeoutOrNull(
                                    5_000L
                                ) {
                                    tryAwaitRelease()
                                }


                            if (
                                releasedBeforeTimeout ==
                                    null
                            ) {

                                showDialog =
                                    true
                            }
                        }
                    )
                }
    ) {

        /*
         * Statuspunkte:
         *
         * Blau = Demo
         * Rot  = Diagnoseaufzeichnung
         *
         * Dadurch bleibt die Versionsnummer frei.
         */
        Row(
            modifier =
                Modifier
                    .align(
                        Alignment.BottomEnd
                    )
                    .padding(
                        end = 2.dp,
                        bottom = 2.dp
                    ),

            horizontalArrangement =
                Arrangement.spacedBy(
                    4.dp
                ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            if (demoEnabled) {

                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .background(
                                color =
                                    DashBlue,

                                shape =
                                    CircleShape
                            )
                )
            }


            if (diagnosticsEnabled) {

                Box(
                    modifier =
                        Modifier
                            .size(7.dp)
                            .background(
                                color =
                                    DashRed,

                                shape =
                                    CircleShape
                            )
                )
            }
        }
    }


    if (showDialog) {

        AlertDialog(

            modifier =
                Modifier
                    .fillMaxWidth(
                        0.88f
                    ),

            properties =
                DialogProperties(
                    usePlatformDefaultWidth =
                        false
                ),

            onDismissRequest = {
                showDialog =
                    false
            },

            title = {

                Text(
                    text =
                        "DriveChronik Service",

                    color =
                        DashText,

                    fontWeight =
                        FontWeight.Bold
                )
            },

            text = {

                Column(
                    verticalArrangement =
                        Arrangement.spacedBy(
                            10.dp
                        )
                ) {

                    Text(
                        text =
                            "BETRIEBSMODUS",

                        color =
                            DashMuted,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            1.sp
                    )


                    Row(
                        modifier =
                            Modifier.fillMaxWidth(),

                        horizontalArrangement =
                            Arrangement.spacedBy(
                                8.dp
                            )
                    ) {

                        Button(
                            onClick = {
                                onDemoEnabledChange(
                                    false
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            if (!demoEnabled)
                                                DashBlue
                                            else
                                                DashPanel,

                                        contentColor =
                                            Color.White
                                    )
                        ) {

                            Text(
                                text =
                                    if (!demoEnabled)
                                        "✓ LIVE"
                                    else
                                        "LIVE"
                            )
                        }


                        Button(
                            onClick = {
                                onDemoEnabledChange(
                                    true
                                )
                            },

                            modifier =
                                Modifier.weight(1f),

                            colors =
                                ButtonDefaults
                                    .buttonColors(
                                        containerColor =
                                            if (demoEnabled)
                                                DashBlue
                                            else
                                                DashPanel,

                                        contentColor =
                                            Color.White
                                    )
                        ) {

                            Text(
                                text =
                                    if (demoEnabled)
                                        "✓ DEMO"
                                    else
                                        "DEMO"
                            )
                        }
                    }


                    Text(
                        text =
                            if (demoEnabled)
                                "Demo aktiv: simulierte Fahr-, Licht- und Navigationswerte."
                            else
                                "Live aktiv: ausschließlich echte Tesla-Daten, keine Demo-Fallbacks.",

                        color =
                            DashMuted,

                        fontSize =
                            12.sp
                    )


                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(
                                    DashLine
                                )
                    )


                    Text(
                        text =
                            "DIAGNOSEAUFZEICHNUNG",

                        color =
                            DashMuted,

                        fontSize =
                            11.sp,

                        fontWeight =
                            FontWeight.Bold,

                        letterSpacing =
                            1.sp
                    )


                    Button(
                        onClick = {

                            onDiagnosticsEnabledChange(
                                !diagnosticsEnabled
                            )
                        },

                        modifier =
                            Modifier.fillMaxWidth(),

                        colors =
                            ButtonDefaults
                                .buttonColors(
                                    containerColor =
                                        if (diagnosticsEnabled)
                                            DashRed
                                        else
                                            DashBlue,

                                    contentColor =
                                        Color.White
                                )
                    ) {

                        Text(
                            text =
                                if (diagnosticsEnabled)
                                    "Diagnose beenden"
                                else
                                    "Diagnose starten"
                        )
                    }


                    Text(
                        text =
                            if (diagnosticsEnabled)
                                "Diagnose läuft. Rohdaten werden lokal auf dem Pixel gespeichert."
                            else
                                "Keine Diagnoseaufzeichnung aktiv.",

                        color =
                            DashMuted,

                        fontSize =
                            12.sp
                    )


                    Text(
                        text =
                            "Diagnose enthält keine VIN, Schlüssel oder Authentifizierungsdaten.",

                        color =
                            DashMuted,

                        fontSize =
                            11.sp
                    )
                }
            },

            confirmButton = {

                Button(
                    onClick = {
                        showDialog =
                            false
                    },

                    colors =
                        ButtonDefaults
                            .buttonColors(
                                containerColor =
                                    DashBlue,

                                contentColor =
                                    Color.White
                            )
                ) {

                    Text(
                        text =
                            "Schließen"
                    )
                }
            },

            containerColor =
                DashPanel
        )
    }
}



@Composable
private fun DashboardHeader() {

    var titleVisible by remember {
        mutableStateOf(true)
    }

    LaunchedEffect(Unit) {
        delay(30_000L)
        titleVisible = false
    }

    val titleAlpha by animateFloatAsState(
        targetValue = if (titleVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "DashboardTitleFade"
    )



    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(30.dp),
        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Text(
            text = "DRIVECHRONIK DASH  ·  v${BuildConfig.VERSION_NAME}",
            color = DashText.copy(alpha = titleAlpha),
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


    var keyAuthorizationMessage by
        remember {
            mutableStateOf<String?>(
                null
            )
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

                context =
                    context.applicationContext,

                expectedLocalNameProvider = {

                    val config =
                        TeslaLiveProtocolSettings(
                            context.applicationContext
                        ).load()


                    val vin =
                        config.normalizedVin


                    if (vin.length == 17) {

                        TeslaBleVehicleIdentity
                            .advertisementLocalName(
                                vin
                            )
                    }
                    else {

                        null
                    }
                }

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
    val expectedConfiguredTeslaName =
        runCatching {

            val vin =
                liveProtocolConfig
                    .normalizedVin

            if (vin.length == 17) {

                TeslaBleVehicleIdentity
                    .advertisementLocalName(
                        vin
                    )
            }
            else {

                null
            }

        }.getOrNull()


    val keyAuthorizationAvailable =
        keyPresent &&
        liveProtocolConfig
            .normalizedVin
            .length == 17 &&
        gattState.status ==
            TeslaGattStatus.READY &&
        scanState.status ==
            TeslaBleScanStatus.FOUND &&
        scanState.name != null &&
        scanState.name ==
            expectedConfiguredTeslaName



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

            onSaveVin = { requestedVin ->

                val normalizedVin =
                    TeslaBleVehicleIdentity
                        .normalizeVin(
                            requestedVin
                        )


                val validVin =
                    Regex(
                        "^[A-HJ-NPR-Z0-9]{17}$"
                    ).matches(
                        normalizedVin
                    )


                if (validVin) {

                    val sameVehicle =
                        normalizedVin ==
                            liveProtocolConfig
                                .normalizedVin


                    if (!sameVehicle) {

                        sessionController
                            ?.clear()

                        keyAuthorizationMessage =
                            null
                    }


                    val updatedConfig =
                        liveProtocolConfig.copy(

                            vin =
                                normalizedVin,

                            /*
                             * Eine neue VIN darf niemals
                             * alte Freigaben übernehmen.
                             */
                            enabled =
                                if (sameVehicle)
                                    liveProtocolConfig.enabled
                                else
                                    false,

                            keyEnrollmentConfirmed =
                                if (sameVehicle)
                                    liveProtocolConfig
                                        .keyEnrollmentConfirmed
                                else
                                    false
                        )


                    liveProtocolSettings
                        .save(
                            updatedConfig
                        )


                    liveProtocolConfig =
                        updatedConfig
                }
            },

            authorizationAvailable =
                keyAuthorizationAvailable,

            authorizationMessage =
                keyAuthorizationMessage,

            onAuthorizeKey = {

                /*
                 * Zweite Sicherheitsprüfung unmittelbar
                 * vor dem tatsächlichen TX.
                 */
                val expectedName =
                    runCatching {

                        TeslaBleVehicleIdentity
                            .advertisementLocalName(
                                liveProtocolConfig
                                    .normalizedVin
                            )

                    }.getOrNull()


                val safeToSend =
                    keyPresent &&
                    liveProtocolConfig
                        .normalizedVin
                        .length == 17 &&
                    gattState.status ==
                        TeslaGattStatus.READY &&
                    scanState.status ==
                        TeslaBleScanStatus.FOUND &&
                    scanState.name != null &&
                    scanState.name ==
                        expectedName


                if (!safeToSend) {

                    keyAuthorizationMessage =
                        "Autorisierung nicht möglich: " +
                        "passender Tesla ist nicht verbunden"
                }
                else {

                    try {

                        /*
                         * Niemals einen alten/manuellen
                         * Bestätigungszustand übernehmen.
                         */
                        val unverifiedConfig =
                            liveProtocolConfig.copy(
                                enabled = false,
                                keyEnrollmentConfirmed =
                                    false
                            )


                        liveProtocolSettings
                            .save(
                                unverifiedConfig
                            )


                        liveProtocolConfig =
                            unverifiedConfig


                        sessionController
                            ?.clear()


                        val enrollmentMessage =
                            TeslaKeyEnrollmentRequest
                                .build(
                                    TeslaKeyStore
                                        .encodedPublicKey()
                                )


                        val accepted =
                            gattClient
                                .sendVcsecMessage(
                                    enrollmentMessage
                                )


                        if (accepted) {

                            keyAuthorizationMessage =
                                "Anfrage gesendet – " +
                                "NFC-Schlüsselkarte oder Keyfob " +
                                "am Tesla auflegen und am " +
                                "Fahrzeugdisplay bestätigen"

                            Log.d(
                                "DriveChronikENROLL",
                                "Tesla add-key request queued " +
                                    "bytes=${enrollmentMessage.size}"
                            )
                        }
                        else {

                            keyAuthorizationMessage =
                                "Autorisierungsanfrage konnte " +
                                "nicht gesendet werden"
                        }
                    }
                    catch (e: Exception) {

                        keyAuthorizationMessage =
                            e.message
                                ?: "Autorisierungsanfrage fehlgeschlagen"

                        Log.e(
                            "DriveChronikENROLL",
                            "Tesla add-key request failed",
                            e
                        )
                    }
                }
            },

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

                keyAuthorizationMessage =
                    null

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

                keyAuthorizationMessage =
                    null

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


            onResetSetup = {

                /*
                 * Nur die lokale Tesla-BLE-Einrichtung
                 * dieses Gerätes zurücksetzen.
                 */
                sessionController
                    ?.clear()


                keyAuthorizationMessage =
                    null


                val resetConfig =
                    TeslaLiveProtocolConfig(
                        enabled = false,
                        keyEnrollmentConfirmed = false,
                        vin = ""
                    )


                liveProtocolSettings
                    .save(
                        resetConfig
                    )


                liveProtocolConfig =
                    resetConfig


                try {

                    TeslaKeyStore
                        .deleteKey()


                    keyPresent =
                        TeslaKeyStore
                            .hasKey()


                    keyError =
                        if (!keyPresent)
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
                            ?: "Lokale Einrichtung konnte nicht vollständig zurückgesetzt werden"
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
                /*
                 * Eine bestätigte Autorisierung gehört
                 * immer exakt zu der gespeicherten VIN.
                 *
                 * Wird die VIN geändert, darf die alte
                 * Bestätigung niemals übernommen werden.
                 */
                val sameVehicle =
                    requested
                        .normalizedVin ==
                    liveProtocolConfig
                        .normalizedVin


                val verifiedEnrollment =
                    requested
                        .keyEnrollmentConfirmed &&
                    keyPresent &&
                    sameVehicle


                val safeConfig =
                    requested.copy(

                        keyEnrollmentConfirmed =
                            verifiedEnrollment,

                        enabled =
                            requested.enabled &&
                            verifiedEnrollment &&
                            requested
                                .normalizedVin
                                .length == 17
                    )


                if (!sameVehicle) {

                    keyAuthorizationMessage =
                        null
                }


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
private fun rememberAutomaticSolarNightMode(): Boolean {

    val context =
        LocalContext.current

    val preferences =
        remember(context) {
            context.getSharedPreferences(
                "drivechronik_solar_theme",
                Context.MODE_PRIVATE
            )
        }


    val storedLatitude =
        preferences
            .getString(
                "last_tesla_latitude",
                null
            )
            ?.toDoubleOrNull()

    val storedLongitude =
        preferences
            .getString(
                "last_tesla_longitude",
                null
            )
            ?.toDoubleOrNull()


    var latitude by
        remember {
            mutableStateOf(
                storedLatitude
                    ?: SolarDayNight.DEFAULT_LATITUDE
            )
        }

    var longitude by
        remember {
            mutableStateOf(
                storedLongitude
                    ?: SolarDayNight.DEFAULT_LONGITUDE
            )
        }


    var lastPersistedLatitude by
        remember {
            mutableStateOf(
                storedLatitude
            )
        }

    var lastPersistedLongitude by
        remember {
            mutableStateOf(
                storedLongitude
            )
        }


    var now by
        remember {
            mutableStateOf(
                java.time.ZonedDateTime.now()
            )
        }


    LaunchedEffect(Unit) {

        while (true) {

            val vehicleLocation =
                TeslaDashboardRepository
                    .snapshot
                    .value
                    .location

            val liveLatitude =
                vehicleLocation
                    ?.latitude
                    ?.toDouble()

            val liveLongitude =
                vehicleLocation
                    ?.longitude
                    ?.toDouble()


            val validLocation =
                liveLatitude != null &&
                    liveLongitude != null &&
                    liveLatitude.isFinite() &&
                    liveLongitude.isFinite() &&
                    liveLatitude in -90.0..90.0 &&
                    liveLongitude in -180.0..180.0


            if (validLocation) {

                val newLatitude =
                    liveLatitude!!

                val newLongitude =
                    liveLongitude!!

                latitude =
                    newLatitude

                longitude =
                    newLongitude


                /*
                 * Nicht alle 30 Sekunden Flash schreiben.
                 * Erst ab ungefähr 0,1 Grad Positionsänderung
                 * oder beim ersten Tesla-Fix speichern.
                 */
                val shouldPersist =
                    lastPersistedLatitude == null ||
                        lastPersistedLongitude == null ||
                        kotlin.math.abs(
                            newLatitude -
                                lastPersistedLatitude!!
                        ) >= 0.1 ||
                        kotlin.math.abs(
                            newLongitude -
                                lastPersistedLongitude!!
                        ) >= 0.1


                if (shouldPersist) {

                    preferences
                        .edit()
                        .putString(
                            "last_tesla_latitude",
                            newLatitude.toString()
                        )
                        .putString(
                            "last_tesla_longitude",
                            newLongitude.toString()
                        )
                        .apply()

                    lastPersistedLatitude =
                        newLatitude

                    lastPersistedLongitude =
                        newLongitude
                }
            }


            now =
                java.time.ZonedDateTime.now()

            /*
             * Mehr als ausreichend genau für
             * Sonnenuntergang / Tagesanbruch.
             */
            delay(
                30_000L
            )
        }
    }


    return remember(
        now,
        latitude,
        longitude
    ) {

        SolarDayNight.isNight(
            now = now,
            latitude = latitude,
            longitude = longitude
        )
    }
}


@Composable
private fun EnergyRing(
    remainingKwh: Float,
    maxKwh: Float,
    modifier: Modifier = Modifier
) {

    val percent =
        if (
            maxKwh >
                0.1f
        ) {

            (
                remainingKwh /
                    maxKwh *
                    100f
            )
                .coerceIn(
                    0f,
                    100f
                )
        }
        else {
            0f
        }


    BoxWithConstraints(
        modifier =
            modifier,

        contentAlignment =
            Alignment.Center
    ) {

        val gaugeSize =
            minOf(
                maxWidth,
                maxHeight
            )


        Canvas(
            modifier =
                Modifier.size(
                    gaugeSize
                )
        ) {

            val canvasSize =
                size.minDimension


            val center =
                androidx.compose.ui.geometry.Offset(
                    size.width /
                        2f,

                    size.height /
                        2f
                )


            /*
             * Der normale Tacho:
             *
             * 0 km/h   = 135°
             * 200 km/h = 405°
             *
             * Damit ist:
             *
             * 100 km/h = 270°
             *
             * Der Energiekranz verwendet also
             * genau 135° von 0 bis 100.
             */
            val startAngle =
                135f

            val fullEnergySweep =
                135f


            /*
             * Etwas innerhalb des äußersten
             * Tachorings, aber außerhalb der
             * Geschwindigkeits-Teilstriche.
             */
            val radius =
                canvasSize *
                    0.465f


            val strokeWidth =
                8.dp.toPx()


            val arcTopLeft =
                androidx.compose.ui.geometry.Offset(
                    center.x -
                        radius,

                    center.y -
                        radius
                )


            val arcSize =
                androidx.compose.ui.geometry.Size(
                    radius *
                        2f,

                    radius *
                        2f
                )


            /*
             * Dunkle Hintergrundspur:
             * kompletter Bereich 0...100 %.
             */
            drawArc(
                color =
                    Color.White.copy(
                        alpha =
                            0.07f
                    ),

                startAngle =
                    startAngle,

                sweepAngle =
                    fullEnergySweep,

                useCenter =
                    false,

                topLeft =
                    arcTopLeft,

                size =
                    arcSize,

                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width =
                            strokeWidth,

                        cap =
                            androidx.compose.ui.graphics.StrokeCap.Butt
                    )
            )



            /*
             * Stufenloser Farbverlauf über feste SOC-Skala.
             * Nur vorhandene Restenergie wird gezeichnet.
             */
            if (percent > 0f) {
                val energyBrush = Brush.sweepGradient(
                    0f to DashRed,
                    (startAngle / 360f) to DashRed,
                    ((startAngle + fullEnergySweep * 0.15f) / 360f)
                        to Color(0xFFFF8800),
                    ((startAngle + fullEnergySweep * 0.35f) / 360f)
                        to Color(0xFFFFD600),
                    ((startAngle + fullEnergySweep * 0.70f) / 360f)
                        to DashGreen,
                    ((startAngle + fullEnergySweep) / 360f)
                        to DashGreen,
                    1f to DashGreen,
                    center = center
                )

                drawArc(
                    brush = energyBrush,
                    startAngle = startAngle,
                    sweepAngle =
                        fullEnergySweep *
                        (percent / 100f),
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style =
                        androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap =
                                androidx.compose.ui.graphics.StrokeCap.Butt
                        )
                )
            }


            /*
             * Heller Endmarker auf dem aktuellen
             * Ladezustand.
             */
            if (
                percent >
                    0f
            ) {

                val markerAngle =
                    startAngle +
                        percent /
                        100f *
                        fullEnergySweep


                val markerRad =
                    markerAngle *
                        kotlin.math.PI
                            .toFloat() /
                        180f


                val markerCenter =
                    androidx.compose.ui.geometry.Offset(

                        x =
                            center.x +
                                kotlin.math.cos(
                                    markerRad
                                ) *
                                radius,

                        y =
                            center.y +
                                kotlin.math.sin(
                                    markerRad
                                ) *
                                radius
                    )


                /*
                 * Dunkler Rand für gute Erkennbarkeit.
                 */
                drawCircle(
                    color =
                        DashBackground,

                    radius =
                        5.5.dp.toPx(),

                    center =
                        markerCenter
                )


                drawCircle(
                    color =
                        Color.White,

                    radius =
                        3.2.dp.toPx(),

                    center =
                        markerCenter
                )
            }
        }
    }
}



@Composable
private fun PowerRing(
    powerKw: Float,
    modifier: Modifier = Modifier
) {
    val target = if (powerKw.isFinite())
        powerKw.coerceIn(-60f, 60f) else 0f

    val power by
        androidx.compose.animation.core.animateFloatAsState(
            targetValue = target,
            animationSpec =
                androidx.compose.animation.core.tween(280),
            label = "PowerRing"
        )

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val gaugeSize = minOf(maxWidth, maxHeight)

        Canvas(Modifier.size(gaugeSize)) {
            val center = Offset(
                size.width / 2f,
                size.height / 2f
            )
            val radius = size.minDimension * 0.465f
            val topLeft = Offset(
                center.x - radius,
                center.y - radius
            )
            val arcSize =
                androidx.compose.ui.geometry.Size(
                    radius * 2f, radius * 2f
                )
            val stroke = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Butt
            )

            // Rechte Hälfte: 270 bis 405 Grad
            drawArc(
                color = Color.White.copy(alpha = 0.07f),
                startAngle = 270f,
                sweepAngle = 135f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = stroke
            )

            val zeroAngle = 337.5f
            val sweep =
                kotlin.math.abs(power) / 60f * 67.5f

            if (sweep > 0f) {
                drawArc(
                    color = if (power < 0f)
                        Color(0xFF4B9CFF)
                    else
                        Color(0xFF36D98B),
                    startAngle = if (power < 0f)
                        zeroAngle - sweep
                    else
                        zeroAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = stroke
                )
            }

            // Nullmarkierung
            val rad = Math.toRadians(
                zeroAngle.toDouble()
            )
            val point = Offset(
                center.x +
                    kotlin.math.cos(rad).toFloat() * radius,
                center.y +
                    kotlin.math.sin(rad).toFloat() * radius
            )

            drawCircle(
                color = DashBackground,
                radius = 5.5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.2.dp.toPx(),
                center = point
            )
        }
    }
}

@Composable
private fun Speedometer(
    speed: Float,
    speedLimit: Int,
    remainingKwh: Float,
    usableCapacityKwh: Float,
    gear: String = "D",
    modifier: Modifier = Modifier
) {


    val energyFraction =
        if (usableCapacityKwh > 0f) {
            (
                remainingKwh /
                    usableCapacityKwh
            ).coerceIn(
                0f,
                1f
            )
        }
        else {
            0f
        }


    val energyPercent =
        (
            energyFraction *
                100f
        ).roundToInt()


    val energyLabel =
        String.format(
            java.util.Locale.GERMANY,
            "%.1f kWh · %d %%",
            remainingKwh
                .coerceAtLeast(0f),
            energyPercent
        )


    val isNightMode =
        rememberAutomaticSolarNightMode()

    val gaugeOuterRingColor =
        if (isNightMode)
            DashNightRing.copy(alpha = 0.82f)
        else
            Color.White.copy(alpha = 0.08f)

    val gaugeInnerRingColor =
        if (isNightMode)
            DashNightRing.copy(alpha = 0.55f)
        else
            Color.White.copy(alpha = 0.07f)

    val gaugeMajorTickColor =
        if (isNightMode)
            DashNightTickMajor
        else
            Color.White.copy(alpha = 0.88f)

    val gaugeMediumTickColor =
        if (isNightMode)
            DashNightTickMedium
        else
            Color.White.copy(alpha = 0.52f)

    val gaugeMinorTickColor =
        if (isNightMode)
            DashNightTickMinor
        else
            Color.White.copy(alpha = 0.28f)

    val gaugeTextColor =
        if (isNightMode)
            android.graphics.Color.argb(
                220,
                174,
                188,
                205
            )
        else
            android.graphics.Color.argb(
                190,
                225,
                229,
                233
            )

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
                        gaugeOuterRingColor,
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
                        gaugeInnerRingColor,
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
                                    gaugeMajorTickColor

                                isMedium ->
                                    gaugeMediumTickColor

                                else ->
                                    gaugeMinorTickColor
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
                            gaugeTextColor

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


                /*
                 * Zahlenwert in der unteren Öffnung.
                 */
                val energyTextPaint =
                    Paint(
                        Paint.ANTI_ALIAS_FLAG
                    ).apply {

                        color =
                            android.graphics.Color.argb(
                                220,
                                226,
                                229,
                                233
                            )

                        textAlign =
                            Paint.Align.CENTER

                        textSize =
                            outerRadius *
                                0.095f

                        typeface =
                            Typeface.create(
                                Typeface.DEFAULT,
                                Typeface.BOLD
                            )
                    }


                drawContext
                    .canvas
                    .nativeCanvas
                    .drawText(
                        energyLabel,
                        center.x,
                        center.y +
                            outerRadius *
                            0.79f,
                        energyTextPaint
                    )
}

            /*
             * Tempolimit im Tachokranz.
             * Kleiner und bewusst zurückhaltend.
             */
            SpeedLimitSign(
                limit =
                    speedLimit,

                modifier =
                    Modifier
                        .align(
                            Alignment.Center
                        )
                        .offset(
                            x = 52.dp,
                            y = (-72).dp
                        )
            )


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
                        text = gear,
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

    /*
     * Kein verifiziertes Tempolimit:
     * kein Schild anzeigen.
     */
    if (limit <= 0) {
        return
    }


    Box(
        modifier =
            modifier
                .size(56.dp)
                .clip(
                    CircleShape
                )
                .background(
                    Color.White.copy(
                        alpha = 0.56f
                    )
                )
                .border(
                    width = 3.dp,
                    color =
                        DashRed.copy(
                            alpha = 0.46f
                        ),
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
                Color.Black.copy(
                    alpha = 0.74f
                ),

            fontSize = 21.sp,
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
private fun NavigationPanel(
    navigation: TeslaNavigationTelemetry,
    modifier: Modifier = Modifier
) {

    val destination =
        navigation.destination
            ?: "Aktive Route"


    val distanceText =
        navigation
            .remainingDistanceKm
            ?.let { km ->

                if (km < 10f) {
                    String.format(
                        java.util.Locale.GERMANY,
                        "%.1f km",
                        km
                    )
                }
                else {
                    "${km.roundToInt()} km"
                }
            }
            ?: "— km"


    val remainingMinutes =
        navigation
            .remainingMinutes
            ?.roundToInt()
            ?.coerceAtLeast(0)


    val durationText =
        remainingMinutes
            ?.let {
                "$it Min"
            }
            ?: "— Min"


    val etaText =
        remainingMinutes
            ?.let { minutes ->

                java.time.LocalTime
                    .now()
                    .plusMinutes(
                        minutes.toLong()
                    )
                    .format(
                        java.time.format
                            .DateTimeFormatter
                            .ofPattern("HH.mm")
                    )
            }
            ?: "—.—"


    val arrivalText =
        navigation
            .energyAtArrivalPercent
            ?.roundToInt()
            ?.coerceIn(
                0,
                100
            )
            ?.let {
                "$it%"
            }
            ?: "—%"


    val trafficDelay =
        navigation
            .trafficDelayMinutes
            ?.roundToInt()
            ?.coerceAtLeast(0)
            ?: 0


    Column(
        modifier =
            modifier
                .padding(
                    start = 14.dp,
                    end = 12.dp,
                    top = 5.dp,
                    bottom = 5.dp
                ),

        verticalArrangement =
            Arrangement.spacedBy(4.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = "⌖",
                color = DashRed,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(
                modifier =
                    Modifier.width(7.dp)
            )

            Text(
                text = destination,
                color = DashText,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }


        Spacer(
            modifier =
                Modifier.height(3.dp)
        )


        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "→ $distanceText",
                color = DashText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            Text(
                text = "→ $durationText",
                color = DashText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }


        Row(
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Text(
                text = "→ $etaText Uhr",
                color = DashText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )

            Text(
                text = "→ mit $arrivalText",
                color = DashText,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
        }


        Spacer(
            modifier =
                Modifier.height(2.dp)
        )


        Text(
            text =
                "↳  Verkehr +$trafficDelay Min",

            color =
                Color(0xFFFFA726),

            fontSize =
                15.sp,

            fontWeight =
                FontWeight.Medium,

            maxLines = 1
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

            TeslaTurnIndicator(
                left = true,
                active = leftIndicator,
                modifier = Modifier.weight(1f)
            )

            TeslaBeamIndicator(
                dipped = true,
                active = lowBeam,
                activeColor =
                    Color(0xFF55C878),
                modifier =
                    Modifier.weight(1f)
            )

            TeslaBeamIndicator(
                dipped = false,
                active = highBeam,
                activeColor =
                    Color(0xFF4A90FF),
                modifier =
                    Modifier.weight(1f)
            )

            TeslaTurnIndicator(
                left = false,
                active = rightIndicator,
                modifier = Modifier.weight(1f)
            )
        }
    }
}


@Composable
private fun TeslaBeamIndicator(
    dipped: Boolean,
    active: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier
) {

    val color =
        if (active)
            activeColor
        else
            Color(0xFF303338)

    Box(
        modifier =
            modifier.fillMaxHeight(),
        contentAlignment =
            Alignment.Center
    ) {

        Canvas(
            modifier =
                Modifier.size(
                    width = 46.dp,
                    height = 38.dp
                )
        ) {

            val sx = size.width / 46f
            val sy = size.height / 38f

            val stroke =
                2.5.dp.toPx()

            val path =
                androidx.compose.ui.graphics.Path().apply {

                    moveTo(
                        25f * sx,
                        5f * sy
                    )

                    lineTo(
                        30f * sx,
                        5f * sy
                    )

                    cubicTo(
                        39f * sx,
                        7f * sy,
                        39f * sx,
                        31f * sy,
                        30f * sx,
                        33f * sy
                    )

                    lineTo(
                        25f * sx,
                        33f * sy
                    )

                    quadraticBezierTo(
                        21f * sx,
                        19f * sy,
                        25f * sx,
                        5f * sy
                    )

                    close()
                }

            drawPath(
                path = path,
                color = color,
                style =
                    androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap =
                            androidx.compose.ui.graphics.StrokeCap.Round,
                        join =
                            androidx.compose.ui.graphics.StrokeJoin.Round
                    )
            )

            val rays =
                listOf(
                    8f,
                    13.5f,
                    19f,
                    24.5f,
                    30f
                )

            for (y in rays) {

                drawLine(
                    color = color,

                    start =
                        androidx.compose.ui.geometry.Offset(
                            5f * sx,
                            (if (dipped) y + 3.5f else y) * sy
                        ),

                    end =
                        androidx.compose.ui.geometry.Offset(
                            19f * sx,
                            y * sy
                        ),

                    strokeWidth = stroke,

                    cap =
                        androidx.compose.ui.graphics.StrokeCap.Round
                )
            }
        }
    }
}


@Composable
private fun TeslaTurnIndicator(
    left: Boolean,
    active: Boolean,
    modifier: Modifier = Modifier
) {

    val indicatorColor =
        if (active)
            Color(0xFF45D177)
        else
            Color(0xFF303338)

    Box(
        modifier = modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
    ) {

        Canvas(
            modifier = Modifier.size(
                width = 42.dp,
                height = 34.dp
            )
        ) {

            val scale = minOf(
                size.width / 48f,
                size.height / 32f
            )

            val offsetX =
                (size.width - 48f * scale) / 2f

            val offsetY =
                (size.height - 32f * scale) / 2f

            fun px(x: Float): Float {
                val mirroredX =
                    if (left) x else 48f - x

                return offsetX + mirroredX * scale
            }

            fun py(y: Float): Float {
                return offsetY + y * scale
            }

            /*
             * Tesla-typischer Richtungspfeil:
             * klarer Pfeilkopf mit geradem Schaft.
             *
             * Rechts wird dieselbe Form gespiegelt.
             */
            val arrow =
                androidx.compose.ui.graphics.Path().apply {

                    moveTo(px(3f), py(16f))

                    lineTo(px(21f), py(2f))
                    lineTo(px(21f), py(10f))

                    lineTo(px(45f), py(10f))
                    lineTo(px(45f), py(22f))

                    lineTo(px(21f), py(22f))
                    lineTo(px(21f), py(30f))

                    close()
                }

            drawPath(
                path = arrow,
                color = indicatorColor
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

    authorizationAvailable:
        Boolean,

    authorizationMessage:
        String?,

    onSaveVin:
        (String) -> Unit,

    onAuthorizeKey:
        () -> Unit,

    onPrepareKey:
        () -> Unit,

    onDeleteVin:
        () -> Unit,

    onDeleteKey:
        () -> Unit,

    onResetSetup:
        () -> Unit,

    onDismiss:
        () -> Unit,

    onSave:
        (TeslaLiveProtocolConfig) -> Unit

) {

    val setupContext =
        LocalContext.current


    val isDebuggable =
        (
            setupContext
                .applicationInfo
                .flags and
                android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
        ) != 0


    /*
     * Reine UI-Vorschau.
     *
     * Verändert weder Tesla-Autorisierung
     * noch SharedPreferences oder BLE.
     */
    var previewStep4 by
        remember {
            mutableStateOf(false)
        }


    var previewLiveEnabled by
        remember {
            mutableStateOf(false)
        }


    var vin by
        remember(
            currentConfig
        ) {
            mutableStateOf(
                currentConfig.vin
            )
        }


    var enrollmentConfirmed by
        remember(
            currentConfig
        ) {
            mutableStateOf(
                currentConfig
                    .keyEnrollmentConfirmed
            )
        }


    var liveEnabled by
        remember(
            currentConfig
        ) {
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



    var confirmResetSetup by
        remember {
            mutableStateOf(false)
        }


    val normalizedVin =
        TeslaBleVehicleIdentity
            .normalizeVin(
                vin
            )


    val vinValid =
        Regex(
            "^[A-HJ-NPR-Z0-9]{17}$"
        ).matches(
            normalizedVin
        )


    val vinSaved =
        vinValid &&
            normalizedVin ==
                currentConfig
                    .normalizedVin


    val livePossible =
        keyPresent &&
            enrollmentConfirmed &&
            vinSaved


    /*
     * Der Assistent bestimmt den nächsten
     * Schritt ausschließlich aus dem
     * tatsächlichen Zustand.
     */
    val activeStep =
        when {

            !vinSaved ->
                1

            !keyPresent ->
                2

            !enrollmentConfirmed ->
                3

            else ->
                4
        }


    val setupComplete =
        activeStep == 4 &&
            liveEnabled &&
            livePossible


    /*
     * Nur die Darstellung kann in der
     * Debug-Version auf Schritt 4 springen.
     *
     * activeStep bleibt unverändert echt.
     */
    val displayStep =
        if (
            isDebuggable &&
            previewStep4
        )
            4
        else
            activeStep


    val cardColor =
        Color(0xFF454A51)

    val mutedColor =
        Color(0xFFC8CCD1)

    val futureColor =
        Color(0xFF777C83)

    val successColor =
        Color(0xFF45D177)

    val activeColor =
        DashRed


    AlertDialog(

        modifier =
            Modifier
                .fillMaxWidth(0.92f),

        properties =
            DialogProperties(
                usePlatformDefaultWidth =
                    false
            ),

        onDismissRequest =
            onDismiss,

        containerColor =
            Color(0xFF3A3F46),

        /*
         * Titel und Hinweis bewusst in
         * einer Zeile – spart Höhe.
         */
        title = {

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

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


                Spacer(
                    modifier =
                        Modifier.width(
                            18.dp
                        )
                )


                Text(
                    text =
                        if (setupComplete)
                            "Einrichtung abgeschlossen · " +
                                "Alle Daten bleiben lokal."
                        else
                            "Einrichtung in 4 Schritten · " +
                                "Alle Daten bleiben lokal auf diesem Gerät.",

                    color =
                        if (setupComplete)
                            successColor
                        else
                            Color(0xFFD3D6DA),

                    fontSize =
                        10.sp,

                    fontWeight =
                        if (setupComplete)
                            FontWeight.Bold
                        else
                            FontWeight.Normal
                )
            }
        },

        text = {

            Column(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalArrangement =
                    Arrangement.spacedBy(
                        7.dp
                    )
            ) {

                /*
                 * ============================================
                 * Fortschrittsleiste
                 * ============================================
                 */
                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    for (step in 1..4) {

                        val done =
                            step < displayStep ||
                                (
                                    step == 4 &&
                                    setupComplete
                                )


                        val active =
                            step == displayStep &&
                                !setupComplete


                        Box(
                            modifier =
                                Modifier
                                    .size(
                                        25.dp
                                    )
                                    .clip(
                                        CircleShape
                                    )
                                    .background(
                                        when {

                                            done ->
                                                successColor

                                            active ->
                                                activeColor

                                            else ->
                                                futureColor
                                        }
                                    ),

                            contentAlignment =
                                Alignment.Center
                        ) {

                            Text(
                                text =
                                    step.toString(),

                                color =
                                    Color.White,

                                fontSize =
                                    11.sp,

                                fontWeight =
                                    FontWeight.Bold
                            )
                        }


                        if (step < 4) {

                            Box(
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .padding(
                                            horizontal =
                                                7.dp
                                        )
                                        .height(
                                            2.dp
                                        )
                                        .background(
                                            if (
                                                step <
                                                activeStep
                                            )
                                                successColor
                                            else
                                                futureColor
                                        )
                            )
                        }
                    }
                }


                Row(
                    modifier =
                        Modifier.fillMaxWidth()
                ) {

                    listOf(
                        "VIN",
                        "Schlüssel",
                        "Autorisieren",
                        "Livedaten"
                    ).forEachIndexed { index, label ->

                        Text(
                            text =
                                label,

                            modifier =
                                Modifier
                                    .weight(1f)
                                    .clickable(
                                        enabled =
                                            isDebuggable &&
                                            label == "Livedaten"
                                    ) {

                                        previewStep4 =
                                            !previewStep4

                                        previewLiveEnabled =
                                            false
                                    },

                            color =
                                if (
                                    displayStep ==
                                    index + 1
                                )
                                    Color.White
                                else
                                    mutedColor,

                            fontSize =
                                9.sp,

                            fontWeight =
                                FontWeight.Bold,

                            textAlign =
                                androidx.compose.ui.text.style
                                    .TextAlign.Center
                        )
                    }
                }


                /*
                 * ============================================
                 * Nur der aktuelle Schritt
                 * ============================================
                 */
                Surface(
                    modifier =
                        Modifier.fillMaxWidth(),

                    shape =
                        RoundedCornerShape(
                            14.dp
                        ),

                    color =
                        cardColor
                ) {

                    Column(
                        modifier =
                            Modifier.padding(
                                horizontal =
                                    18.dp,
                                vertical =
                                    12.dp
                            ),

                        verticalArrangement =
                            Arrangement.spacedBy(
                                7.dp
                            )
                    ) {

                        when (displayStep) {

                            /*
                             * ====================================
                             * SCHRITT 1 – VIN
                             * ====================================
                             */
                            1 -> {

                                Row(
                                    modifier =
                                        Modifier.fillMaxWidth(),

                                    verticalAlignment =
                                        Alignment.CenterVertically
                                ) {

                                    Text(
                                        text =
                                            "1. VIN eingeben",

                                        modifier =
                                            Modifier.weight(1f),

                                        color =
                                            Color.White,

                                        fontSize =
                                            18.sp,

                                        fontWeight =
                                            FontWeight.Bold
                                    )


                                    Text(
                                        text =
                                            "AKTUELL",

                                        color =
                                            activeColor,

                                        fontSize =
                                            10.sp,

                                        fontWeight =
                                            FontWeight.Bold
                                    )
                                }


                                Text(
                                    text =
                                        "Bitte die 17-stellige " +
                                            "Fahrzeug-Identnummer eingeben.",

                                    color =
                                        mutedColor,

                                    fontSize =
                                        10.sp
                                )


                                Text(
                                    text =
                                        "VIN",

                                    color =
                                        Color.White,

                                    fontSize =
                                        10.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                /*
                                 * Bewusst KEIN Material
                                 * OutlinedTextField.
                                 *
                                 * Dadurch wird die VIN auf
                                 * dem Pixel nicht vertikal
                                 * abgeschnitten.
                                 */
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(
                                                54.dp
                                            )
                                            .border(
                                                width =
                                                    2.dp,

                                                color =
                                                    DashRed,

                                                shape =
                                                    RoundedCornerShape(
                                                        6.dp
                                                    )
                                            )
                                            .padding(
                                                horizontal =
                                                    15.dp
                                            ),

                                    contentAlignment =
                                        Alignment.CenterStart
                                ) {

                                    BasicTextField(

                                        value =
                                            vin,

                                        onValueChange = { value ->

                                            val newVin =
                                                value
                                                    .filter {
                                                        it.isLetterOrDigit()
                                                    }
                                                    .take(17)
                                                    .uppercase(
                                                        java.util.Locale.ROOT
                                                    )


                                            vin =
                                                newVin


                                            confirmVinDeletion =
                                                false


                                            val validNewVin =
                                                Regex(
                                                    "^[A-HJ-NPR-Z0-9]{17}$"
                                                ).matches(
                                                    newVin
                                                )


                                            if (
                                                validNewVin &&
                                                newVin !=
                                                    currentConfig
                                                        .normalizedVin
                                            ) {

                                                onSaveVin(
                                                    newVin
                                                )
                                            }
                                        },

                                        modifier =
                                            Modifier.fillMaxWidth(),

                                        singleLine =
                                            true,

                                        textStyle =
                                            TextStyle(

                                                color =
                                                    Color.White,

                                                fontSize =
                                                    17.sp,

                                                lineHeight =
                                                    22.sp,

                                                fontWeight =
                                                    FontWeight.Medium
                                            ),

                                        cursorBrush =
                                            SolidColor(
                                                DashRed
                                            )
                                    )
                                }


                                Text(
                                    text =
                                        when {

                                            normalizedVin.length <
                                                17 ->

                                                "${normalizedVin.length}/17 Zeichen"

                                            vinValid ->

                                                "VIN vollständig – wird gespeichert …"

                                            else ->

                                                "Ungültige VIN: " +
                                                    "I, O und Q sind nicht zulässig"
                                        },

                                    color =
                                        when {

                                            vinValid ->
                                                successColor

                                            normalizedVin.length ==
                                                17 ->
                                                Color(0xFFFFB4AB)

                                            else ->
                                                mutedColor
                                        },

                                    fontSize =
                                        10.sp,

                                    fontWeight =
                                        if (
                                            normalizedVin.length ==
                                            17
                                        )
                                            FontWeight.Bold
                                        else
                                            FontWeight.Normal
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
                                                    "Gespeicherte VIN löschen",

                                            color =
                                                Color(0xFFFFB4AB),

                                            fontSize =
                                                9.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }
                            }


                            /*
                             * ====================================
                             * SCHRITT 2 – KEY
                             * ====================================
                             */
                            2 -> {

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(
                                                120.dp
                                            )
                                ) {

                                    /*
                                     * Texte links
                                     */
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(0.62f)
                                                .align(
                                                    Alignment.CenterStart
                                                ),

                                        verticalArrangement =
                                            Arrangement.spacedBy(
                                                9.dp
                                            )
                                    ) {

                                        Text(
                                            text =
                                                "2. Lokalen Schlüssel erzeugen",

                                            color =
                                                Color.White,

                                            fontSize =
                                                18.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        Text(
                                            text =
                                                "✓ VIN gespeichert",

                                            color =
                                                successColor,

                                            fontSize =
                                                11.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        Text(
                                            text =
                                                "Der Schlüssel wird ausschließlich " +
                                                "lokal auf diesem Gerät erzeugt.",

                                            color =
                                                mutedColor,

                                            fontSize =
                                                10.sp
                                        )


                                        if (keyError != null) {

                                            Text(
                                                text =
                                                    keyError,

                                                color =
                                                    Color(0xFFFFB4AB),

                                                fontSize =
                                                    10.sp
                                            )
                                        }
                                    }


                                    /*
                                     * Button garantiert
                                     * mittig rechts in der KPI.
                                     */
                                    Button(
                                        onClick =
                                            onPrepareKey,

                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.CenterEnd
                                                )
                                                .width(
                                                    220.dp
                                                )
                                                .height(
                                                    48.dp
                                                ),

                                        colors =
                                            ButtonDefaults
                                                .buttonColors(

                                                    containerColor =
                                                        DashRed,

                                                    contentColor =
                                                        Color.White
                                                )
                                    ) {

                                        Text(
                                            text =
                                                "Schlüssel erzeugen",

                                            fontSize =
                                                11.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }
                            }


                            /*
                             * ====================================
                             * SCHRITT 3 – AUTORISIERUNG
                             * ====================================
                             */
                            3 -> {

                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .height(
                                                130.dp
                                            )
                                ) {

                                    /*
                                     * Linke Seite
                                     */
                                    Column(
                                        modifier =
                                            Modifier
                                                .fillMaxWidth(0.64f)
                                                .align(
                                                    Alignment.CenterStart
                                                ),

                                        verticalArrangement =
                                            Arrangement.spacedBy(
                                                8.dp
                                            )
                                    ) {

                                        Text(
                                            text =
                                                "3. Schlüssel in deinem Fahrzeug autorisieren",

                                            color =
                                                Color.White,

                                            fontSize =
                                                18.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        Text(
                                            text =
                                                "✓ VIN gespeichert   ·   " +
                                                "✓ Lokaler Schlüssel vorhanden",

                                            color =
                                                successColor,

                                            fontSize =
                                                10.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        Text(
                                            text =
                                                if (
                                                    authorizationAvailable
                                                )
                                                    "Tesla verbunden – bereit zur Autorisierung"
                                                else
                                                    "Warte auf den zu dieser VIN gehörenden Tesla …",

                                            color =
                                                if (
                                                    authorizationAvailable
                                                )
                                                    successColor
                                                else
                                                    mutedColor,

                                            fontSize =
                                                10.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )


                                        if (
                                            authorizationMessage != null
                                        ) {

                                            Text(
                                                text =
                                                    authorizationMessage,

                                                color =
                                                    Color.White,

                                                fontSize =
                                                    9.sp
                                            )
                                        }
                                    }


                                    /*
                                     * Autorisieren-Button
                                     * mittig rechts
                                     */
                                    Button(
                                        onClick =
                                            onAuthorizeKey,

                                        enabled =
                                            authorizationAvailable,

                                        modifier =
                                            Modifier
                                                .align(
                                                    Alignment.CenterEnd
                                                )
                                                .width(
                                                    220.dp
                                                )
                                                .height(
                                                    48.dp
                                                ),

                                        colors =
                                            ButtonDefaults
                                                .buttonColors(

                                                    containerColor =
                                                        DashRed,

                                                    contentColor =
                                                        Color.White,

                                                    disabledContainerColor =
                                                        Color(0xFF62666C),

                                                    disabledContentColor =
                                                        Color(0xFFBFC2C7)
                                                )
                                    ) {

                                        Text(
                                            text =
                                                "Autorisierung starten",

                                            fontSize =
                                                11.sp,

                                            fontWeight =
                                                FontWeight.Bold
                                        )
                                    }
                                }
                            }


                            /*
                             * ====================================
                             * SCHRITT 4 – LIVE
                             * ====================================
                             */
                            4 -> {

                                val shownLiveEnabled =
                                    if (previewStep4)
                                        previewLiveEnabled
                                    else
                                        liveEnabled


                                val shownLivePossible =
                                    if (previewStep4)
                                        true
                                    else
                                        livePossible


                                Text(
                                    text =
                                        if (previewStep4)
                                            "4. Tesla-Livedaten aktivieren · Vorschau"
                                        else if (setupComplete)
                                            "Einrichtung abgeschlossen"
                                        else
                                            "4. Tesla-Livedaten aktivieren",

                                    color =
                                        Color.White,

                                    fontSize =
                                        18.sp,

                                    fontWeight =
                                        FontWeight.Bold
                                )


                                Text(
                                    text =
                                        "✓ VIN   ·   ✓ Schlüssel   ·   " +
                                            "✓ Tesla-Autorisierung",

                                    color =
                                        successColor,

                                    fontSize =
                                        10.sp,

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
                                            shownLiveEnabled,

                                        enabled =
                                            shownLivePossible,

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

                                            if (previewStep4) {

                                                previewLiveEnabled =
                                                    checked
                                            }
                                            else {

                                                liveEnabled =
                                                    checked
                                            }
                                        }
                                    )


                                    Text(
                                        text =
                                            "Tesla-Livedaten über Bluetooth aktivieren",

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
                                        if (shownLiveEnabled)
                                            "Livedaten aktiviert – " +
                                                "mit Speichern übernehmen."
                                        else
                                            "Livedaten sind noch deaktiviert.",

                                    color =
                                        if (shownLiveEnabled)
                                            successColor
                                        else
                                            mutedColor,

                                    fontSize =
                                        10.sp
                                )
                            }
                        }
                    }
                }
            }
        },

        dismissButton = {

            Button(
                onClick = {

                    if (confirmResetSetup) {

                        onResetSetup()


                        vin =
                            ""

                        enrollmentConfirmed =
                            false

                        liveEnabled =
                            false

                        previewStep4 =
                            false

                        previewLiveEnabled =
                            false

                        confirmVinDeletion =
                            false

                        confirmKeyDeletion =
                            false

                        confirmResetSetup =
                            false
                    }
                    else {

                        confirmResetSetup =
                            true
                    }
                },

                modifier =
                    Modifier
                        .width(
                            260.dp
                        )
                        .height(
                            48.dp
                        ),

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                Color(0xFFF32227),

                            contentColor =
                                Color.White
                        )
            ) {

                Text(
                    text =
                        if (confirmResetSetup)
                            "Wirklich zurücksetzen"
                        else
                            "Einrichtung zurücksetzen",

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        },


        confirmButton = {

            Button(

                enabled =
                    previewStep4 ||
                    activeStep != 4 ||
                    liveEnabled,

                onClick = {

                    if (previewStep4) {

                        previewStep4 =
                            false

                        previewLiveEnabled =
                            false
                    }
                    else if (activeStep == 4) {

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
                    }
                    else {

                        onDismiss()
                    }
                },

                modifier =
                    Modifier
                        .width(
                            260.dp
                        )
                        .height(
                            48.dp
                        ),

                colors =
                    ButtonDefaults
                        .buttonColors(

                            containerColor =
                                Color(0xFF43D17A),

                            contentColor =
                                Color.White
                        )
            ) {

                Text(
                    text =
                        if (previewStep4)
                            "Vorschau schließen"
                        else if (activeStep == 4)
                            "Aktivieren & speichern"
                        else
                            "Schließen",

                    maxLines =
                        1,

                    fontSize =
                        11.sp,

                    fontWeight =
                        FontWeight.Bold
                )
            }
        }
    )
}
