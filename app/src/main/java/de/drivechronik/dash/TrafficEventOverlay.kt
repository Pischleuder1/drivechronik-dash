package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun TrafficEventOverlay(
    modifier: Modifier = Modifier
) {
    val event =
        TrafficEventState.events
            .sortedWith(
                compareBy<TrafficEvent> {
                    when {
                        it.blocked -> 0
                        it.type == TrafficEventType.CLOSURE -> 1
                        it.type == TrafficEventType.WARNING -> 2
                        else -> 3
                    }
                }.thenBy { it.road }
            )
            .firstOrNull()
            ?: return

    var visible by remember(event.id) {
        mutableStateOf(true)
    }

    LaunchedEffect(event.id) {
        visible = true
        delay(6_000L)
        visible = false
    }

    if (!visible) return

    val isClosure =
        event.blocked ||
            event.type == TrafficEventType.CLOSURE

    val headline =
        when {
            event.blocked ->
                "⛔ SPERRUNG · ${event.road}"

            event.type == TrafficEventType.CLOSURE ->
                "⚠ Einschränkung · ${event.road}"

            event.type == TrafficEventType.WARNING ->
                "⚠ Verkehr · ${event.road}"

            else ->
                "🚧 Baustelle · ${event.road}"
        }

    Surface(
        modifier = modifier.widthIn(
            min = 210.dp,
            max = 300.dp
        ),
        shape = RoundedCornerShape(14.dp),
        color =
            if (isClosure)
                Color(0xFF4A2323)
            else
                Color(0xFF3A3F46),
        shadowElevation = 10.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 14.dp,
                vertical = 12.dp
            ),
            verticalArrangement =
                Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = headline,
                color =
                    if (isClosure)
                        Color(0xFFFF8D8D)
                    else
                        Color(0xFFFFC66D),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )

            if (event.title.isNotBlank()) {
                Text(
                    text = event.title,
                    color = Color.White,
                    fontSize = 15.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3
                )
            }

            if (event.subtitle.isNotBlank()) {
                Text(
                    text = event.subtitle,
                    color = Color(0xFFC9D0D6),
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    maxLines = 2
                )
            }
        }
    }
}
