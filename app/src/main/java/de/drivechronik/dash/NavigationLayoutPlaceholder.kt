package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val navigationCurve = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val inset = minOf(w * 0.12f, h * 0.18f)

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    lineTo(0f, h)
    cubicTo(0f, h * 0.72f, inset, h * 0.75f,
        inset, h * 0.5f)
    cubicTo(inset, h * 0.25f, 0f, h * 0.28f,
        0f, 0f)
    close()
}

@Composable
internal fun NavigationLayoutPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(vertical = 7.dp)
            .clip(navigationCurve)
            .background(Color(0xFF192B3C))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 58.dp,
                    end = 10.dp,
                    top = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "NAVIGATION",
                color = Color(0xFF76C9FF),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Kartenansicht",
                color = Color.White,
                fontSize = 18.sp
            )
            Text(
                "Tesla / Android",
                color = Color(0xFFB0C6D8),
                fontSize = 12.sp
            )
            Text(
                "Sprachsteuerung: DriveChronik",
                color = Color(0xFFB0C6D8),
                fontSize = 11.sp
            )
        }
    }
}
