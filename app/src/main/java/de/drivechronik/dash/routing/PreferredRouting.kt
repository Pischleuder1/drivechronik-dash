package de.drivechronik.dash.routing

import android.content.Context
import kotlin.math.*

/**
 * Primäres Routing:
 *
 * 1. Online über OSRM
 * 2. Bei Fehler automatisch Offline-Valhalla
 *
 * Die bestehende OfflineRoute-Struktur bleibt erhalten,
 * damit Navigation, Manöveranzeige und Rerouting unverändert
 * weiterarbeiten können.
 */
suspend fun calculatePreferredRoute(
    context: Context,
    start: RoutePoint,
    end: RoutePoint,
    startHeading: Float? = null
): Result<OfflineRoute> {

    val avoidHighways =
        de.drivechronik.dash.NavigationHighwaySetting
            .enabled(context)

    /*
     * Autobahn vermeiden:
     *
     * Online-Valhalla zuerst, weil es use_highways=0.0
     * direkt unterstützt.
     *
     * Dafür sind keinerlei Offline-Pakete erforderlich.
     */
    if (avoidHighways) {
        val valhalla =
            calculateOnlineValhallaRoute(
                start = start,
                end = end,
                startHeading = startHeading
            )

        val route =
            valhalla.getOrNull()

        if (route != null &&
            route.points.size >= 2
        ) {
            android.util.Log.i(
                "DashRouting",
                "Online-Valhalla Autobahnvermeidung: " +
                    "%.1f km / %.0f min".format(
                        route.distanceKm,
                        route.durationSeconds / 60.0
                    )
            )

            return Result.success(route)
        }

        android.util.Log.w(
            "DashRouting",
            "Online-Valhalla fehlgeschlagen: " +
                (
                    valhalla.exceptionOrNull()
                        ?.message
                        ?: "unbekannter Fehler"
                    ) +
                " -> OSRM-Fallback"
        )
    }

    /*
     * Normales Online-Routing bzw. Fallback,
     * falls Online-Valhalla nicht erreichbar ist.
     */
    val online =
        runCatching {
            calculateRoute(
                start = start,
                destination = end
            )
        }.getOrNull()

    if (online != null &&
        online.points.size >= 2
    ) {
        return runCatching {
            online.toOfflineRoute(end)
        }
    }

    /*
     * Letzte Stufe:
     * vorhandenes Offline-Valhalla.
     *
     * Ist kein Offline-Routing installiert,
     * liefert calculateOfflineRoute sauber einen Fehler.
     */
    return calculateOfflineRoute(
        context = context,
        start = start,
        end = end,
        startHeading = startHeading
    )
}


/**
 * OSRM DrivingRoute -> bestehendes DriveChronik-Routenmodell.
 */
private fun DrivingRoute.toOfflineRoute(
    destination: RoutePoint
): OfflineRoute {

    val routePoints = points

    fun nearestShapeIndex(point: RoutePoint): Int {
        var bestIndex = 0
        var bestDistance = Double.MAX_VALUE

        routePoints.forEachIndexed { index, candidate ->
            val lat = Math.toRadians(
                (candidate.lat + point.lat) / 2.0
            )

            val dx = Math.toRadians(
                candidate.lon - point.lon
            ) * cos(lat)

            val dy = Math.toRadians(
                candidate.lat - point.lat
            )

            val distance = hypot(dx, dy)

            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }

        return bestIndex
    }

    val maneuvers = steps.mapNotNull { step ->
        val beginIndex =
            nearestShapeIndex(step.point)

        val endIndex =
            nearestShapeIndex(step.endPoint)

        val type = osrmToValhallaType(
            step.type,
            step.direction
        )

        val instruction = osrmInstruction(
            step.type,
            step.direction,
            step.road,
            step.exitNumber
        )

        if (instruction.isBlank()) {
            null
        } else {
            RouteManeuver(
                instruction = instruction,
                type = type,
                distanceKm = step.distanceM / 1000.0,
                durationSeconds = 0.0,
                beginShapeIndex = beginIndex,
                endShapeIndex =
                    maxOf(beginIndex, endIndex)
            )
        }
    }

    return OfflineRoute(
        points = routePoints,
        distanceKm = distanceM / 1000.0,
        durationSeconds = durationS,
        maneuvers = maneuvers,
        destination = destination
    )
}


private fun osrmToValhallaType(
    type: String,
    modifier: String
): Int {

    val t = type.lowercase()
    val m = modifier.lowercase()

    if (t == "roundabout" ||
        t == "rotary" ||
        t == "roundabout turn"
    ) {
        return 26
    }

    if (t == "uturn" ||
        m == "uturn"
    ) {
        return 25
    }

    return when (m) {
        "sharp right" -> 13
        "right" -> 10
        "slight right" -> 9

        "sharp left" -> 12
        "left" -> 14
        "slight left" -> 16

        "straight" -> 1

        else -> 1
    }
}


private fun osrmInstruction(
    type: String,
    modifier: String,
    road: String,
    exitNumber: Int? = null
): String {

    val t = type.lowercase()
    val m = modifier.lowercase()

    val target =
        road.trim().takeIf { it.isNotEmpty() }
            ?.let { " auf $it" }
            ?: ""

    if (t == "roundabout" ||
        t == "rotary" ||
        t == "roundabout turn"
    ) {
        return if (exitNumber != null) {
            "In den Kreisverkehr einfahren und die " +
                "$exitNumber. Ausfahrt$target nehmen"
        } else {
            "In den Kreisverkehr einfahren$target"
        }
    }

    if (t == "depart") {
        return "Geradeaus fahren$target"
    }

    if (t == "arrive") {
        return "Ziel erreicht"
    }

    return when (m) {
        "sharp right" ->
            "Scharf rechts abbiegen$target"

        "right" ->
            "Rechts abbiegen$target"

        "slight right" ->
            "Leicht rechts abbiegen$target"

        "sharp left" ->
            "Scharf links abbiegen$target"

        "left" ->
            "Links abbiegen$target"

        "slight left" ->
            "Leicht links abbiegen$target"

        "uturn" ->
            "Wenden$target"

        "straight" ->
            "Geradeaus fahren$target"

        else ->
            if (target.isNotEmpty())
                "Weiterfahren$target"
            else
                "Weiterfahren"
    }
}
