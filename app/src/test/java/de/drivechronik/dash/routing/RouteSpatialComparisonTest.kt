package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RouteSpatialComparisonTest {
    @Test
    fun indexedAndFullScanAgree() {
        val routes = listOf(
            (0..3000).map {
                RoutePoint(52.0 + it * 0.0001,
                    8.0 + it * 0.0002)
            },
            listOf(
                RoutePoint(52.0, 8.0),
                RoutePoint(52.001, 8.001),
                RoutePoint(52.0, 8.002),
                RoutePoint(52.001, 8.0),
                RoutePoint(52.0, 8.001)
            )
        )

        for (points in routes) {
            val route = OfflineRoute(
                points, 1.0, 100.0, emptyList()
            )
            for (p in points.filterIndexed {
                i, _ -> i % 50 == 0
            }) {
                for (offset in listOf(
                    0.0, 0.0002, 0.002, 0.02
                )) {
                    val position = RoutePoint(
                        p.lat + offset, p.lon
                    )
                    for (heading in listOf(
                        null, 0f, 90f, 180f
                    )) {
                        val full = calculateRouteProgress(
                            route, position,
                            headingDegrees = heading,
                            useSpatialIndex = false
                        )
                        val indexed = calculateRouteProgress(
                            route, position,
                            headingDegrees = heading
                        )
                        assertEquals(full, indexed)
                    }
                }
            }
        }
    }
}
