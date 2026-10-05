package de.drivechronik.dash.routing

import kotlin.math.*

data class RouteProgress(
    val segmentIndex: Int,
    val distanceAlongRouteMeters: Double,
    val distanceRemainingMeters: Double,
    val distanceFromRouteMeters: Double,
    val distanceToManeuverMeters: Double?,
    val nextManeuver: RouteManeuver?
)

private fun distanceMeters(a: RoutePoint, b: RoutePoint): Double {
    val lat = Math.toRadians((a.lat + b.lat) / 2.0)
    val dx = Math.toRadians(b.lon - a.lon) * cos(lat)
    val dy = Math.toRadians(b.lat - a.lat)
    return 6371000.0 * hypot(dx, dy)
}

fun calculateRouteProgress(
    route: OfflineRoute,
    position: RoutePoint,
    minProgressMeters: Double = 0.0,
    headingDegrees: Float? = null,
    useSpatialIndex: Boolean = true
): RouteProgress? {
    val points = route.points
    if (points.size < 2) return null

    val lengths = RouteLengthCache.get(route)

    var bestDistance = Double.MAX_VALUE
    var bestProgress = 0.0
    var bestSegment = 0

    fun scan(indices: Iterable<Int>) {
        for (i in indices) {
        val a = points[i]
        val b = points[i + 1]
        val scale = cos(Math.toRadians(position.lat))
        val ax = (a.lon - position.lon) * scale
        val ay = a.lat - position.lat
        val bx = (b.lon - a.lon) * scale
        val by = b.lat - a.lat
        val len2 = bx * bx + by * by

        val t = if (len2 > 0.0) {
            ((-ax * bx - ay * by) / len2).coerceIn(0.0, 1.0)
        } else 0.0

        val d = 111195.0 * hypot(ax + t * bx, ay + t * by)

        val progress = lengths[i] +
            t * (lengths[i + 1] - lengths[i])

        if (progress < minProgressMeters) continue

        val headingValid = headingDegrees?.takeIf {
            it.isFinite() && it in 0f..360f
        }

        if (headingValid != null && len2 > 0.0) {
            val segmentHeading = (
                Math.toDegrees(atan2(bx, by)) + 360.0
            ) % 360.0
            val difference = abs(
                (segmentHeading - headingValid + 540.0) %
                    360.0 - 180.0
            )
            if (difference > 75.0) continue
        }

        if (d < bestDistance) {
            bestDistance = d
            bestSegment = i
            bestProgress = lengths[i] +
                t * (lengths[i + 1] - lengths[i])
        }
    }

    }

    if (useSpatialIndex) {
        val nearby = RouteSpatialIndexCache.get(route)
            .nearby(position)
        scan(nearby)
    }

    if (bestDistance > 100.0) {
        scan(0 until points.lastIndex)
    }

    // Kein passender Routenabschnitt gefunden.
    if (bestDistance == Double.MAX_VALUE) return null

    val next = route.maneuvers.firstOrNull {
        val index = it.beginShapeIndex
        index in points.indices &&
            lengths[index] > bestProgress + 1.0
    }

    return RouteProgress(
        segmentIndex = bestSegment,
        distanceAlongRouteMeters = bestProgress,
        distanceRemainingMeters = (lengths.last() - bestProgress).coerceAtLeast(0.0),
        distanceFromRouteMeters = bestDistance,
        distanceToManeuverMeters = next?.let {
            (lengths[it.beginShapeIndex] - bestProgress)
                .coerceAtLeast(0.0)
        },
        nextManeuver = next
    )
}


private object RouteLengthCache {
    private var cachedRoute: OfflineRoute? = null
    private var cachedLengths: DoubleArray? = null

    @Synchronized
    fun get(route: OfflineRoute): DoubleArray {
        if (cachedRoute === route) {
            cachedLengths?.let { return it }
        }

        val points = route.points
        val lengths = DoubleArray(points.size)

        for (i in 1 until points.size) {
            lengths[i] = lengths[i - 1] +
                distanceMeters(points[i - 1], points[i])
        }

        cachedRoute = route
        cachedLengths = lengths
        return lengths
    }
}
