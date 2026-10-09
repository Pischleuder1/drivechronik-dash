package de.drivechronik.dash

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
internal fun NavigationTurnPopup(
    modifier: Modifier = Modifier
) {
    val hint = NavigationHintState.hint

    var phase by remember(hint?.id) {
        mutableIntStateOf(0)
    }
    var popupVisible by remember(hint?.id) {
        mutableStateOf(false)
    }

    val valid = hint != null &&
        !hint.stale &&
        hint.distance.isFinite() &&
        hint.distance >= 0.0

    LaunchedEffect(hint?.id, hint?.distance, hint?.stale) {
        if (!valid) {
            popupVisible = false
            return@LaunchedEffect
        }

        val distance = hint!!.distance

        when {
            distance <= 125.0 && phase < 2 -> {
                phase = 2
                popupVisible = true
            }
            distance <= 500.0 && phase == 0 -> {
                phase = 1
                popupVisible = true
            }
        }
    }

    LaunchedEffect(hint?.id, phase) {
        if (phase > 0) {
            delay(3_000L)
            popupVisible = false
        }
    }

    AnimatedVisibility(
        visible = valid && popupVisible,
        modifier = modifier,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        if (hint != null) {
            val info = remember(hint.instruction) {
                formatManeuver(hint.instruction)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 245.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF3A3F46),
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 28.dp
                    ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val exitNumber =
                        if (isRoundaboutInstruction(
                                hint.type, hint.instruction
                            ))
                            extractRoundaboutExit(hint.instruction)
                        else null

                    val graphic =
                        NavigationHintState.roundaboutGraphic
                            ?.takeIf { it.hintId == hint.id }

                    if (exitNumber != null && graphic != null) {
                        RealRoundaboutIndicator(graphic)
                    } else if (exitNumber != null) {
                        NavigationRoundaboutIndicator(
                            exitNumber = exitNumber,
                            modifier = Modifier
                        )
                    } else {
                        ManeuverArrow(
                            type = hint.type,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(Modifier.width(26.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement =
                            Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = String.format(
                                Locale.GERMANY,
                                "In %.0f m",
                                hint.distance
                            ),
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (hint.distance < 100)
                                Color(0xFFFFBF6E)
                            else Color(0xFF65B9FF)
                        )

                        Text(
                            text = info.action,
                            fontSize = 29.sp,
                            lineHeight = 35.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (info.detail.isNotBlank()) {
                            Text(
                                text = info.detail,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF65B9FF)
                            )
                        }

                        if (info.street.isNotBlank()) {
                            Text(
                                text = info.street,
                                fontSize = 23.sp,
                                lineHeight = 29.sp,
                                color = Color.White
                            )
                        }

                        if (info.roadNumber.isNotBlank()) {
                            Text(
                                text = info.roadNumber,
                                fontSize = 22.sp,
                                color = Color(0xFFC3CBD3)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManeuverArrow(
    type: Int,
    modifier: Modifier = Modifier
) {
    val symbol = navigationTurnArrow(type)

    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val blue = Color(0xFF65B9FF)
        val stroke = 12.dp.toPx()

        fun point(x: Float, y: Float) =
            Offset(w * x, h * y)

        fun line(
            x1: Float, y1: Float,
            x2: Float, y2: Float
        ) {
            drawLine(
                color = blue,
                start = point(x1, y1),
                end = point(x2, y2),
                strokeWidth = stroke,
                cap = StrokeCap.Round
            )
        }

        when (symbol) {
            "↻" -> {
                drawArc(
                    color = blue,
                    startAngle = 45f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = point(.18f, .18f),
                    size = androidx.compose.ui.geometry.Size(
                        w * .64f, h * .64f
                    ),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round
                    )
                )
                line(.68f, .16f, .83f, .30f)
                line(.83f, .30f, .86f, .12f)
            }

            "↱", "↗", "⇢", "↷" -> {
                line(.35f, .90f, .35f, .43f)
                line(.35f, .43f, .83f, .43f)
                line(.83f, .43f, .63f, .23f)
                line(.83f, .43f, .63f, .63f)
            }

            "↰", "↖", "↶" -> {
                line(.65f, .90f, .65f, .43f)
                line(.65f, .43f, .17f, .43f)
                line(.17f, .43f, .37f, .23f)
                line(.17f, .43f, .37f, .63f)
            }

            else -> {
                line(.50f, .90f, .50f, .14f)
                line(.50f, .14f, .29f, .36f)
                line(.50f, .14f, .71f, .36f)
            }
        }
    }
}
