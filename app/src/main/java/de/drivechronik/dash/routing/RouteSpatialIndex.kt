package de.drivechronik.dash.routing

import kotlin.math.*

internal class RouteSpatialIndex(
    private val route: OfflineRoute
) {
    private val cellSize = 0.01
    private val cells = HashMap<Pair<Int, Int>, MutableList<Int>>()

    private fun cell(value: Double): Int =
        floor(value / cellSize).toInt()

    init {
        val points = route.points

        for (i in 0 until points.lastIndex) {
            val a = points[i]
            val b = points[i + 1]

            val y0 = cell(min(a.lat, b.lat))
            val y1 = cell(max(a.lat, b.lat))
            val x0 = cell(min(a.lon, b.lon))
            val x1 = cell(max(a.lon, b.lon))

            for (y in y0..y1) {
                for (x in x0..x1) {
                    cells.getOrPut(y to x) {
                        ArrayList()
                    }.add(i)
                }
            }
        }
    }

    fun nearby(
        position: RoutePoint,
        radiusMeters: Double = 120.0
    ): List<Int> {
        val latMargin = radiusMeters / 111000.0
        val scale = cos(Math.toRadians(position.lat))
            .coerceAtLeast(0.01)
        val lonMargin = radiusMeters / (111000.0 * scale)

        val result = LinkedHashSet<Int>()

        for (y in cell(position.lat - latMargin)..
            cell(position.lat + latMargin)) {
            for (x in cell(position.lon - lonMargin)..
                cell(position.lon + lonMargin)) {
                cells[y to x]?.let { result.addAll(it) }
            }
        }

        return result.toList().sorted()
    }
}

internal object RouteSpatialIndexCache {
    private var route: OfflineRoute? = null
    private var index: RouteSpatialIndex? = null

    @Synchronized
    fun get(value: OfflineRoute): RouteSpatialIndex {
        if (route === value) {
            index?.let { return it }
        }
        return RouteSpatialIndex(value).also {
            route = value
            index = it
        }
    }
}
