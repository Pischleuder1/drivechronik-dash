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
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date

@Composable
internal fun NavigationGuidance(
    route: OfflineRoute?,
    manualRouteGeneration: Int,
    onRecalculated: (OfflineRoute, OfflineRoute) -> Unit,
    onArrived: (OfflineRoute) -> Unit,
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
        var rerouteJob: Job? = null
        var arrivalCount = 0
        var arrivalSince = 0L

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

                    // Zielankunft vor jedem Rerouting prüfen.
                    val target = route.destination
                        ?: route.points.last()
                    val meters = FloatArray(1)
                    Location.distanceBetween(
                        fix.point.lat, fix.point.lon,
                        target.lat, target.lon,
                        meters
                    )

                    val arrivalProgress = progress
                    val arrivedNearEnd =
                        arrivalProgress != null &&
                        arrivalProgress.distanceRemainingMeters <= 20.0 &&
                        arrivalProgress.distanceFromRouteMeters <= 20.0 &&
                        meters[0] <= 25f

                    if (arrivedNearEnd) {
                        if (arrivalCount == 0) {
                            arrivalSince = now
                        }
                        arrivalCount++
                    } else {
                        arrivalCount = 0
                        arrivalSince = 0L
                    }

                    if (arrivalCount >= 3 &&
                        now - arrivalSince >= 2_000L
                    ) {
                        rerouteJob?.cancel()
                        NavigationHintState.clear()
                        onArrived(route)
                        return@LaunchedEffect
                    }

                    // Valhalla nicht in der GPS-Schleife abwarten.
                    if (rerouteJob?.isActive != true) {
                        rerouteJob = launch {
                            if (coordinator.onFix(fix.point, now, fix.heading)) {
                                NavigationHintState.clear()
                                onRecalculated(
                                    route, coordinator.route
                                )
                            }
                        }
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

    // Kreisverkehr-Geometrie einmal je Fahrmanöver
    // prüfen, nicht bei jedem GPS-Tick.
    LaunchedEffect(
        route,
        manualRouteGeneration,
        progress?.nextManeuver?.beginShapeIndex
    ) {
        val maneuver = progress?.nextManeuver
            ?: return@LaunchedEffect

        if (!isRoundaboutInstruction(
                maneuver.type,
                maneuver.instruction
            )) return@LaunchedEffect

        val point = route.points.getOrNull(
            maneuver.beginShapeIndex
        ) ?: return@LaunchedEffect

        val match = RoundaboutIndex.find(
            context, point
        )

        val traversal = match?.let {
            resolveRoundaboutTraversal(
                it, route, maneuver
            )
        }

        val activeHint = NavigationHintState.hint

        if (
            match != null &&
            traversal != null &&
            traversal.orderedExits.size in 2..5 &&
            activeHint != null &&
            !activeHint.stale &&
            activeHint.instruction == maneuver.instruction &&
            activeHint.type == maneuver.type
        ) {
            NavigationHintState.setRoundaboutGraphic(
                activeHint.id, match, traversal
            )
        }

        android.util.Log.i(
            "DriveChronikRoundabout",
            if (traversal == null)
                "TRAVERSAL: keine eindeutige Zuordnung"
            else
                "TRAVERSAL: " +
                    "Einfahrt=${traversal.entry} " +
                    "Ausfahrt=${traversal.exit} " +
                    "Nummer=${traversal.exitNumber} " +
                    "Ausfahrten=${traversal.orderedExits}"
        )

        if (match != null) {
            val begin = maneuver.beginShapeIndex
            val end = maneuver.endShapeIndex

            android.util.Log.i(
                "DriveChronikRoundabout",
                "ROUTE begin=$begin end=$end " +
                    "points=${route.points.size}"
            )

            for (i in
                (begin - 8).coerceAtLeast(0)..
                (end + 8).coerceAtMost(
                    route.points.lastIndex
                )
            ) {
                val p = route.points[i]

                android.util.Log.i(
                    "DriveChronikRoundabout",
                    "POINT $i ${p.lat},${p.lon}"
                )
            }
        }

        android.util.Log.i(
            "DriveChronikRoundabout",
            if (match == null)
                "Kein eindeutiger Kreisverkehr: " +
                    maneuver.instruction
            else
                "Treffer=${match.id} " +
                    "Abstand=${match.distanceMeters.toInt()}m " +
                    "Einfahrten=${match.entries.size} " +
                    "Ausfahrtsknoten=${match.exits.size} " +
                    "Valhalla=${maneuver.instruction}"
        )
    }

    NavigationGuidancePanel(
        route,
        if (offRouteDetected) null else progress
    )
}
