package de.drivechronik.dash

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import de.drivechronik.dash.routing.calculateOfflineRoute
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import de.drivechronik.dash.routing.OfflineRoute
import androidx.compose.ui.Alignment
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
    nightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var avoidHighways by remember {
        mutableStateOf(NavigationHighwaySetting.enabled(context))
    }
    var highwayRequest by remember {
        mutableIntStateOf(0)
    }

    var routePoints by remember {
        mutableStateOf<OfflineRoute?>(null)
    }

    var manualRouteGeneration by remember {
        mutableIntStateOf(0)
    }

    val (phoneState, _, _) =
        rememberPhoneWeatherLocation(navigation = true)

    LaunchedEffect(NavigationSimulation.active) {
        while (NavigationSimulation.active) {
            val now = android.os.SystemClock.elapsedRealtime()
            delay(if (NavigationSimulation.paused) 1000L else 100L)
            val later = android.os.SystemClock.elapsedRealtime()
            NavigationSimulation.advance(
                ((later - now) / 1000.0).coerceIn(0.0, 0.25)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { NavigationSimulation.stop() }
    }

    val stopNavigation = {
        NavigationSimulation.stop()
        manualRouteGeneration++
        NavigationHintState.clear()
        NavigationSummaryState.clearDestination()
        routePoints = null
    }

    Box(
        modifier = modifier
            .padding(vertical = 7.dp)
            .clip(navigationCurve)
            .background(Color(0xFF192B3C))
    ) {
        NavigationMap(
            nightMode = nightMode,
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints?.points.orEmpty(),
            phoneState = phoneState
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 10.dp, start = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationSimulationControls(
                routePoints,
                onStop = stopNavigation
            )

            if (routePoints != null) {
                IconButton(
                    onClick = stopNavigation,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0xFFC0393E),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(
                        "×",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            AutobahnAvoidButton(
                active = avoidHighways,
                onClick = {
                    val requested = !avoidHighways
                    avoidHighways = requested
                    NavigationHighwaySetting.set(context, requested)
                    highwayRequest++
                    val request = highwayRequest
                    val previous = routePoints

                    if (previous != null) {
                        val origin =
                            currentNavigationFix(phoneState.value)?.point
                        val target = previous.destination
                            ?: previous.points.lastOrNull()

                        if (origin == null || target == null) {
                            Toast.makeText(
                                context,
                                "Neuberechnung: GPS fehlt",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            scope.launch {
                                val result = calculateOfflineRoute(
                                    context, origin, target
                                )
                                if (request != highwayRequest ||
                                    routePoints !== previous
                                ) return@launch

                                result.onSuccess { updated ->
                                    NavigationSimulation.followReroute(updated)
                                    manualRouteGeneration++
                                    routePoints = updated
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Route konnte nicht neu berechnet werden",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            )
        }

        NavigationGuidance(
            routePoints,
            manualRouteGeneration = manualRouteGeneration,
            phoneState = phoneState,
            onRecalculated = { original, updated ->
                if (routePoints === original) {
                    NavigationSimulation.followReroute(updated)
                    routePoints = updated
                }
            },
            onArrived = { arrived ->
                if (routePoints === arrived) {
                    stopNavigation()
                }
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 8.dp)
        ) {
            NavigationSearch(
                onRoute = { route ->
                    NavigationSimulation.stop()
                    manualRouteGeneration++
                    NavigationHintState.clear()
                    routePoints = route
                },
                onStop = stopNavigation,
                navigationActive = routePoints != null,
                phoneState = phoneState
            )
        }

    }
}


@Composable
private fun AutobahnAvoidButton(
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                if (active) Color(0xFF276A98)
                else Color(0xEE142B40),
                RoundedCornerShape(10.dp)
            )
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val k = size.width / 72f
            drawRoundRect(
                color = Color(0xFF0755A5),
                cornerRadius = CornerRadius(6 * k)
            )
            val road = Path().apply {
                moveTo(22*k, 60*k)
                lineTo(29*k, 12*k)
                lineTo(34*k, 12*k)
                lineTo(31*k, 60*k)
                close()
                moveTo(50*k, 60*k)
                lineTo(43*k, 12*k)
                lineTo(38*k, 12*k)
                lineTo(41*k, 60*k)
                close()
            }
            drawPath(road, Color.White)
            drawLine(
                Color.White, Offset(11*k, 35*k),
                Offset(27*k, 35*k), 4*k
            )
            drawLine(
                Color.White, Offset(45*k, 35*k),
                Offset(61*k, 35*k), 4*k
            )
            if (active) {
                drawLine(
                    Color(0xFFFF4444),
                    Offset(12*k, 62*k),
                    Offset(60*k, 10*k),
                    strokeWidth = 8*k,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
