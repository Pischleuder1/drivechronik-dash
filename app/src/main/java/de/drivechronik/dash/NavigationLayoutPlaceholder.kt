package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
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
    modifier: Modifier = Modifier
) {
    var routePoints by remember {
        mutableStateOf<OfflineRoute?>(null)
    }

    var manualRouteGeneration by remember {
        mutableIntStateOf(0)
    }

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

    Box(
        modifier = modifier
            .padding(vertical = 7.dp)
            .clip(navigationCurve)
            .background(Color(0xFF192B3C))
    ) {
        NavigationMap(
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints?.points.orEmpty()
        )

        routePoints?.let { route ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 36.dp, end = 8.dp)
            ) {
                NavigationSimulationControls(route)
            }
        }

        NavigationGuidance(
            routePoints,
            manualRouteGeneration = manualRouteGeneration,
            onRecalculated = { original, updated ->
                if (routePoints === original) {
                    NavigationSimulation.stop()
                    routePoints = updated
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
                }
            )
        }

    }
}
