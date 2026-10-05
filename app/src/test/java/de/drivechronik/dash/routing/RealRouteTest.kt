package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RealRouteTest {
    @Test fun compareRealRoute() {
        val stream = requireNotNull(
            javaClass.getResourceAsStream(
                "/real_route_points.csv"
            )
        )
        val points = stream.bufferedReader().use {
            it.readLines().drop(1).map { line ->
                val v = line.split(",")
                RoutePoint(v[0].toDouble(), v[1].toDouble())
            }
        }

        assertTrue(points.size > 8000)

        val route = OfflineRoute(
            points, 635.449, 22980.0, emptyList()
        )

        var checked = 0

        for (i in points.indices step 160) {
            val p = points[i]

            for (offset in listOf(
                0.0, 0.00025, 0.002
            )) {
                val pos = RoutePoint(
                    p.lat + offset, p.lon
                )

                for (heading in listOf(
                    null, 90f, 180f
                )) {
                    val full = calculateRouteProgress(
                        route, pos,
                        headingDegrees = heading,
                        useSpatialIndex = false
                    )
                    val indexed = calculateRouteProgress(
                        route, pos,
                        headingDegrees = heading
                    )

                    assertEquals(
                        "Abweichung bei Punkt $i",
                        full, indexed
                    )
                    checked++
                }
            }
        }
        println("VERGLICHE=$checked")
        println("ROUTENPUNKTE=${points.size}")
    }

    @Test fun crossingTieComparison() {
        val points = listOf(
            RoutePoint(52.0, 8.0),
            RoutePoint(52.002, 8.002),
            RoutePoint(52.0, 8.002),
            RoutePoint(52.002, 8.0)
        )
        val route = OfflineRoute(
            points, 1.0, 100.0, emptyList()
        )
        val position = RoutePoint(52.001, 8.001)

        for (minimum in listOf(0.0, 50.0, 200.0)) {
            for (heading in listOf(null, 45f, 135f)) {
                val full = calculateRouteProgress(
                    route, position, minimum, heading,
                    useSpatialIndex = false
                )
                val indexed = calculateRouteProgress(
                    route, position, minimum, heading,
                    useSpatialIndex = true
                )
                assertEquals(full, indexed)
            }
        }
    }
}
