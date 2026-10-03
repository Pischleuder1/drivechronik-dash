package de.drivechronik.dash

import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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

        Box(
            modifier =
                Modifier
                    .size(7.dp)
                    .background(
                        DashMuted,
                        CircleShape
                    )
        )

        Spacer(
            modifier =
                Modifier.width(5.dp)
        )

        Text(
            text = "Tesla BLE",
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
