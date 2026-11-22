package de.drivechronik.dash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

private val HistoryBackground = Color(0xFF14202C)
private val HistoryOrange = Color(0xFFFFA347)
private val HistoryBlue = Color(0xFF55B9F5)
private val HistoryGrid = Color(0xFF344453)
private val HistoryMuted = Color(0xFF98AABB)

@Composable
internal fun TechnikPowerHistory(
    powerKw: Float,
    demoMode: Boolean,
    modifier: Modifier = Modifier
) {
    val samples = remember {
        mutableStateListOf<Float?>()
    }

    val currentPower by rememberUpdatedState(
        powerKw.takeIf { it.isFinite() }
    )

    LaunchedEffect(demoMode) {
        samples.clear()

        while (true) {
            samples.add(currentPower)

            if (samples.size > 60) {
                samples.removeAt(0)
            }

            delay(1_000L)
        }
    }

    val values = samples.toList()
    val latest = values.lastOrNull()

    Column(
        modifier = modifier
            .background(
                HistoryBackground,
                RoundedCornerShape(12.dp)
            )
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "LEISTUNG · 60 s",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )

            Text(
                text = latest?.let {
                    "${if (it > 0f) "+" else ""}${it.roundToInt()} kW"
                } ?: "— kW",
                color = when {
                    latest == null -> HistoryMuted
                    latest < 0f -> HistoryBlue
                    else -> HistoryOrange
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        val maxMagnitude = max(
            40f,
            values.filterNotNull()
                .maxOfOrNull { abs(it) }
                ?: 40f
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val width = size.width
            val height = size.height
            val middleY = height / 2f

            if (width <= 0f || height <= 0f) {
                return@Canvas
            }

            // Null-Linie und Orientierungslinien.
            for (fraction in listOf(0.25f, 0.5f, 0.75f)) {
                val y = height * fraction

                drawLine(
                    color = HistoryGrid,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // 60 Sekunden fester Zeitbereich.
            // Die neuesten Daten liegen immer rechts.
            fun x(index: Int): Float =
                width * (index + 60 - values.size) / 59f

            fun y(value: Float): Float =
                middleY -
                    (value / maxMagnitude) *
                    (height * 0.47f)

            // Nur gültige, aufeinanderfolgende Messwerte.
            // Beim Vorzeichenwechsel Linien an der
            // Null-Linie sauber in Grün/Blau aufteilen.
            for (i in 1 until values.size) {
                val previous = values[i - 1] ?: continue
                val current = values[i] ?: continue

                val x0 = x(i - 1)
                val x1 = x(i)
                val y0 = y(previous)
                val y1 = y(current)

                if (
                    (previous >= 0f && current >= 0f) ||
                    (previous <= 0f && current <= 0f)
                ) {
                    drawLine(
                        color =
                            if (previous < 0f || current < 0f)
                                HistoryBlue
                            else
                                HistoryOrange,
                        start = Offset(x0, y0),
                        end = Offset(x1, y1),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                } else {
                    val ratio =
                        abs(previous) /
                            (abs(previous) + abs(current))

                    val zeroX = x0 + (x1 - x0) * ratio
                    val zero = Offset(zeroX, middleY)

                    drawLine(
                        color =
                            if (previous < 0f)
                                HistoryBlue
                            else
                                HistoryOrange,
                        start = Offset(x0, y0),
                        end = zero,
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color =
                            if (current < 0f)
                                HistoryBlue
                            else
                                HistoryOrange,
                        start = zero,
                        end = Offset(x1, y1),
                        strokeWidth = 2.5.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "−60 s",
                color = HistoryMuted,
                fontSize = 9.sp
            )
            Text(
                "● Antrieb",
                color = HistoryOrange,
                fontSize = 9.sp
            )
            Text(
                "● Rekup",
                color = HistoryBlue,
                fontSize = 9.sp
            )
            Text(
                "Jetzt",
                color = HistoryMuted,
                fontSize = 9.sp
            )
        }
    }
}
