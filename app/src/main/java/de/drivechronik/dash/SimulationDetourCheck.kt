package de.drivechronik.dash

import de.drivechronik.dash.routing.*
import kotlin.math.*

internal fun validSimulationDetour(
    original: OfflineRoute,
    candidate: OfflineRoute,
    start: RoutePoint
): Boolean {
    val points = candidate.points
    if (points.size < 3) return false

    fun meters(a: RoutePoint, b: RoutePoint): Double {
        val lat = Math.toRadians((a.lat + b.lat) / 2.0)
        return 6371000.0 * hypot(
            Math.toRadians(b.lon - a.lon) * cos(lat),
            Math.toRadians(b.lat - a.lat)
        )
    }

    // Kein großer Sprung beim Simulationsstart.
    if (meters(start, points.first()) > 25.0)
        return false

    // Keine extrem lange Umwegstrecke.
    if (candidate.distanceKm !in 0.2..5.0)
        return false

    // Die Strecke muss die alte Route deutlich verlassen.
    val step = maxOf(1, points.size / 40)
    val deviates = points.indices.step(step).any { i ->
        val progress = calculateRouteProgress(
            original, points[i]
        )
        (progress?.distanceFromRouteMeters ?: 0.0) > 120.0
    }

    return deviates
}
