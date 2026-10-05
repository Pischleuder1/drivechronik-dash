package de.drivechronik.dash

import android.location.Location

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drivechronik.dash.routing.*
import kotlinx.coroutines.delay
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun NavigationGuidance(
    route: OfflineRoute?,
    manualRouteGeneration: Int,
    onRecalculated: (OfflineRoute, OfflineRoute) -> Unit,
    phoneState: State<Location?>
) {
    if (route == null) return

    // Gemeinsames Navigations-GPS
    val context = LocalContext.current
    val tracker = remember(route, manualRouteGeneration) { RouteTracking() }
    val coordinator = remember(context, manualRouteGeneration) {
        RerouteCoordinator(route) { start, target ->
            calculateOfflineRoute(
                context,
                start,
                target,
                startHeading = if (NavigationSimulation.active)
                    NavigationSimulation.fix?.heading
                else null
            )
        }
    }
    var offRouteDetected by remember(route, manualRouteGeneration) {
        mutableStateOf(false)
    }
    var progress by remember(route, manualRouteGeneration) {
        mutableStateOf<RouteProgress?>(null)
    }

    LaunchedEffect(
        route,
        manualRouteGeneration,
        NavigationSimulation.generation
    ) {
        coordinator.adoptRoute(route)
        NavigationHintState.clear()
        var lastHintKey = ""
        var lastSource = ""
        var lastFixNanos: Long? = null
        var lastAcceptedAt: Long? = null
        var gpsInterrupted = false
        val handover = NavigationGpsHandover()

        while (true) {
            val fix = currentNavigationFix(phoneState.value)

            if (fix == null) {
                handover.reset()
                progress = null
                gpsInterrupted = true
                NavigationHintState.markStale()
            } else if (!handover.accept(fix)) {
                progress = null
                NavigationHintState.markStale()
            } else {
                val isNew = fix.source != lastSource ||
                    fix.timestampNanos != lastFixNanos

                if (isNew) {
                    if (gpsInterrupted) {
                        tracker.reset()
                        coordinator.resetDetection()
                        lastAcceptedAt = null
                        gpsInterrupted = false
                    }
                    val now =
                        android.os.SystemClock.elapsedRealtime()

                    val elapsed = lastAcceptedAt?.let {
                        (now - it).coerceAtLeast(0L) / 1000.0
                    } ?: 1.0

                    if (fix.source != lastSource) {
                        tracker.reset()
                        coordinator.resetDetection()
                    }

                    progress = tracker.update(
                        route,
                        fix.point,
                        elapsed,
                        fix.heading
                    )

                    if (coordinator.onFix(fix.point, now)) {
                        NavigationHintState.clear()
                        onRecalculated(route, coordinator.route)
                        return@LaunchedEffect
                    }
                    offRouteDetected = coordinator.isOffRoute
                    if (offRouteDetected) {
                        lastHintKey = ""
                        NavigationHintState.clear()
                    }

                    lastSource = fix.source
                    lastFixNanos = fix.timestampNanos
                    if (progress != null) {
                        lastAcceptedAt = now
                    }
                }
            }
            val validProgress =
                progress.takeUnless { offRouteDetected }
            val maneuver = validProgress?.nextManeuver
            val distance = validProgress?.distanceToManeuverMeters

            if (maneuver != null && distance != null) {
                val key = "${maneuver.beginShapeIndex}:" +
                    "${maneuver.type}:${maneuver.instruction}"

                if (key != lastHintKey) {
                    lastHintKey = key
                    NavigationHintState.show(
                        maneuver.instruction,
                        maneuver.type,
                        distance
                    )
                } else {
                    NavigationHintState.updateDistance(distance)
                }
            } else if (offRouteDetected ||
                progress != null) {
                lastHintKey = ""
                NavigationHintState.clear()
            } else {
                NavigationHintState.markStale()
            }

            delay(1000)
        }
    }

    NavigationGuidancePanel(
        route,
        if (offRouteDetected) null else progress
    )
}
