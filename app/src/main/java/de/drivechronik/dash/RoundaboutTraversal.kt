package de.drivechronik.dash

import de.drivechronik.dash.routing.OfflineRoute
import de.drivechronik.dash.routing.RouteManeuver
import de.drivechronik.dash.routing.RoutePoint
import kotlin.math.*

internal data class RoundaboutTraversal(
    val entry: Int,
    val exit: Int,
    val orderedExits: List<Int>,
    val exitNumber: Int
)

internal fun resolveRoundaboutTraversal(
    match: RoundaboutMatch,
    route: OfflineRoute,
    maneuver: RouteManeuver
): RoundaboutTraversal? {
    val start = route.points.getOrNull(
        maneuver.beginShapeIndex
    ) ?: return null

    val end = route.points.getOrNull(
        maneuver.endShapeIndex
    ) ?: return null

    fun meters(a: RoutePoint, b: RoutePoint): Double {
        val lat = Math.toRadians((a.lat + b.lat) / 2)
        return 6371000.0 * hypot(
            Math.toRadians(b.lon - a.lon) * cos(lat),
            Math.toRadians(b.lat - a.lat)
        )
    }

    fun nearest(
        point: RoutePoint,
        indices: List<Int>
    ): Int? = indices
        .filter { it in match.ring.indices }
        .minByOrNull { meters(point, match.ring[it]) }
        ?.takeIf {
            meters(point, match.ring[it]) <= 15.0
        }

    val entry = nearest(start, match.entries)
        ?: return null
    val exit = nearest(end, match.exits)
        ?: return null

    val count = match.ring.size

    val ordered = match.exits
        .filter { it in 0 until count && it != entry }
        .distinct()
        .sortedBy { (it - entry + count) % count }

    val number = ordered.indexOf(exit) + 1
    if (number <= 0) return null

    val announced = extractRoundaboutExit(
        maneuver.instruction
    ) ?: return null

    // Niemals der Valhalla-Ansage widersprechen.
    if (announced != number) return null

    return RoundaboutTraversal(
        entry = entry,
        exit = exit,
        orderedExits = ordered,
        exitNumber = number
    )
}
