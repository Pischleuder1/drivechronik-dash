package de.drivechronik.dash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal fun extractRoundaboutExit(text: String): Int? {
    val lower = text.lowercase()

    val regexDigits = Regex("""(\d+)\.\s*ausfahrt""")
    regexDigits.find(lower)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
        return it
    }

    val words = mapOf(
        "erste" to 1,
        "zweite" to 2,
        "dritte" to 3,
        "vierte" to 4,
        "fünfte" to 5,
        "sechste" to 6
    )

    for ((word, value) in words) {
        if (lower.contains("$word ausfahrt")) return value
    }

    return null
}

internal fun isRoundaboutInstruction(type: Int, text: String): Boolean {
    val lower = text.lowercase()
    return type == 26 ||
        lower.contains("kreisverkehr") ||
        lower.contains("roundabout")
}

@Composable
internal fun NavigationRoundaboutIndicator(
    exitNumber: Int,
    totalExits: Int = maxOf(4, exitNumber).coerceAtMost(6),
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(92.dp)) {
        Canvas(modifier = Modifier.size(92.dp)) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val ringRadius = size.minDimension * 0.23f
            val outerRadius = size.minDimension * 0.42f
            val spokeStroke = size.minDimension * 0.06f
            val ringStroke = size.minDimension * 0.10f

            val ringColor = Color(0xFF65B9FF)
            val inactiveColor = Color(0xFF6A7682)
            val activeColor = Color.White

            // Ring
            drawCircle(
                color = ringColor,
                radius = ringRadius,
                center = Offset(cx, cy),
                style = Stroke(width = ringStroke)
            )

            // Einfahrt unten
            drawLine(
                color = inactiveColor,
                start = Offset(cx, cy + outerRadius),
                end = Offset(cx, cy + ringRadius + ringStroke * 0.4f),
                strokeWidth = spokeStroke,
                cap = StrokeCap.Round
            )

            // Fahrtrichtung als Bogen im Kreis
            drawArc(
                color = ringColor,
                startAngle = 90f,
                sweepAngle = -250f,
                useCenter = false,
                topLeft = Offset(cx - ringRadius, cy - ringRadius),
                size = Size(ringRadius * 2, ringRadius * 2),
                style = Stroke(width = spokeStroke * 0.75f, cap = StrokeCap.Round)
            )
        }

        Text(
            text = exitNumber.toString(),
            modifier = Modifier.align(Alignment.Center),
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
