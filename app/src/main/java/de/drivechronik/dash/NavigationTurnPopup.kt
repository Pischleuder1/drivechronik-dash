package de.drivechronik.dash

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
    val liveHint = NavigationHintState.hint
    val completedHintId = NavigationHintState.completedHintId
    val latestHint by rememberUpdatedState(liveHint)

    var shownHint by remember {
        mutableStateOf<NavigationHint?>(null)
    }
    var shownRoundabout by remember {
        mutableStateOf<RoundaboutGraphic?>(null)
    }
    var activeId by remember {
        mutableStateOf<Long?>(null)
    }
    var outgoingId by remember {
        mutableStateOf<Long?>(null)
    }
    var phase by remember {
        mutableIntStateOf(0)
    }
    var popupVisible by remember {
        mutableStateOf(false)
    }

    val liveValid = liveHint != null &&
        !liveHint.stale &&
        liveHint.distance.isFinite() &&
        liveHint.distance >= 0.0

    LaunchedEffect(
        liveHint?.id, liveHint?.distance,
        liveHint?.stale, completedHintId
    ) {
        if (!liveValid) {
            if (completedHintId != null &&
                shownHint?.id == completedHintId &&
                phase == 2 && popupVisible) {
                // Letztes Manöver: 1 s halten, danach Fade-out.
                outgoingId = completedHintId
                return@LaunchedEffect
            }

            outgoingId = null
            shownHint = null
            shownRoundabout = null
            activeId = null
            phase = 0
            popupVisible = false
            return@LaunchedEffect
        }

        val incoming = liveHint ?: return@LaunchedEffect

        if (activeId != incoming.id) {
            activeId = incoming.id

            if (outgoingId == null) {
                if (phase == 2 && popupVisible &&
                    shownHint != null && incoming.distance > 125.0) {
                    // Manöverwechsel: alten Hinweis kurz behalten.
                    outgoingId = shownHint?.id
                } else {
                    // Neuer Hinweis oder unmittelbar nächste Abbiegung.
                    shownHint = incoming
                    shownRoundabout = null
                    phase = 0
                    popupVisible = false
                }
            }
        }

        if (outgoingId != null) {
            if (incoming.distance <= 125.0) {
                // Unmittelbar folgende Abbiegung hat Vorrang.
                outgoingId = null
                shownHint = incoming
                shownRoundabout = null
                phase = 2
                popupVisible = true
            }
            return@LaunchedEffect
        }

        shownHint = incoming
        shownRoundabout = NavigationHintState.roundaboutGraphic
            ?.takeIf { it.hintId == incoming.id }

        when {
            incoming.distance <= 125.0 && phase < 2 -> {
                phase = 2
                popupVisible = true
            }
            incoming.distance <= 500.0 && phase == 0 -> {
                phase = 1
                popupVisible = true
            }
        }
    }

    // Nach erfasstem Manöverwechsel:
    // 1 s stehen lassen, danach 1,2 s weich ausblenden.
    LaunchedEffect(outgoingId) {
        val endingId = outgoingId ?: return@LaunchedEffect

        delay(1_000L)
        if (outgoingId != endingId) return@LaunchedEffect

        popupVisible = false
        // Animationsinhalt bis zum Ende des Fade-outs behalten.
        delay(2_000L)

        if (outgoingId != endingId) return@LaunchedEffect

        outgoingId = null
        phase = 0

        val next = latestHint
        if (next != null && !next.stale &&
            next.distance.isFinite() && next.distance >= 0.0) {
            shownHint = next
            shownRoundabout = NavigationHintState.roundaboutGraphic
                ?.takeIf { it.hintId == next.id }
            when {
                next.distance <= 125.0 -> {
                    phase = 2
                    popupVisible = true
                }
                next.distance <= 500.0 -> {
                    phase = 1
                    popupVisible = true
                }
                else -> popupVisible = false
            }
        } else {
            shownHint = null
            shownRoundabout = null
            popupVisible = false
        }
    }

    // Nur die 500-m-Vorankündigung auf 3 s begrenzen.
    LaunchedEffect(activeId, phase, outgoingId) {
        if (phase == 1 && outgoingId == null) {
            delay(3_000L)
            if (phase == 1 && outgoingId == null) {
                popupVisible = false
            }
        }
    }

    // Während der Nachlaufphase den alten Hinweis unabhängig
    // von der Entfernung des neuen Manövers anzeigen.
    val valid = shownHint != null &&
        (liveValid || outgoingId != null)

    AnimatedVisibility(
        visible = valid && popupVisible,
        modifier = modifier,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300)
        ) { it } + fadeIn(
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            animationSpec = tween(
                durationMillis = 2000
            )
        ) { it / 4 } + fadeOut(
            animationSpec = tween(
                durationMillis = 2000
            )
        )
    ) {
        shownHint?.let { hint ->
            val info = remember(hint.instruction) {
                formatManeuver(hint.instruction)
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 390.dp)
                    .padding(bottom = 12.dp),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF3A3F46),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical = 24.dp
                    ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    val isRoundabout =
                        isRoundaboutInstruction(
                            hint.type,
                            hint.instruction
                        )

                    val graphic =
                        if (isRoundabout)
                            shownRoundabout
                                ?.takeIf { it.hintId == hint.id }
                        else
                            null

                    val announcedExitNumber =
                        if (isRoundabout)
                            extractRoundaboutExit(
                                hint.instruction
                            )
                        else
                            null

                    /*
                     * Die echte Kreisverkehr-Geometrie hat Vorrang.
                     *
                     * Dadurch funktioniert die Ausfahrtsanzeige
                     * unabhängig vom Routinganbieter:
                     * - OSRM online
                     * - Valhalla online
                     * - Valhalla offline
                     *
                     * Nur wenn keine Geometrie verfügbar ist,
                     * verwenden wir weiterhin die Nummer aus
                     * dem Ansagetext.
                     */
                    if (graphic != null) {
                        RealRoundaboutIndicator(graphic)
                    } else if (announcedExitNumber != null) {
                        NavigationRoundaboutIndicator(
                            exitNumber = announcedExitNumber,
                            modifier = Modifier
                        )
                    } else {
                        ManeuverArrow(
                            type = hint.type,
                            modifier = Modifier.size(120.dp)
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement =
                            Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.Start
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
