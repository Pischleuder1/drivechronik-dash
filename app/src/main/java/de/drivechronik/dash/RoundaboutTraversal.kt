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
    )

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

    val count = match.ring.size

    val ordered = match.exits
        .filter { it in 0 until count && it != entry }
        .distinct()
        .sortedBy { (it - entry + count) % count }

    if (ordered.isEmpty()) return null

    val announced = extractRoundaboutExit(
        maneuver.instruction
    )

    /*
     * Bevorzugt die vom Router angekündigte Ausfahrtnummer.
     *
     * Das ist insbesondere für OSRM wichtig:
     * dessen Step-Geometrie kann nach dem Kreisverkehr
     * noch bis zum nächsten Fahrmanöver weiterlaufen.
     *
     * Die eigentliche Pfeilgeometrie stammt trotzdem
     * weiterhin aus dem lokalen OSM-Kreisverkehr.
     *
     * Funktioniert identisch für:
     * - OSRM online
     * - Valhalla online
     * - Valhalla offline
     */
    if (announced != null) {
        if (announced !in 1..ordered.size) {
            return null
        }

        val exit = ordered[announced - 1]

        return RoundaboutTraversal(
            entry = entry,
            exit = exit,
            orderedExits = ordered,
            exitNumber = announced
        )
    }

    /*
     * Fallback für Router/Manöver ohne angekündigte
     * Ausfahrtnummer: geometrische Zuordnung über
     * den Endpunkt des Fahrmanövers.
     */
    val exit = end?.let {
        nearest(it, match.exits)
    } ?: return null

    val number = ordered.indexOf(exit) + 1
    if (number <= 0) return null

    return RoundaboutTraversal(
        entry = entry,
        exit = exit,
        orderedExits = ordered,
        exitNumber = number
    )
}
