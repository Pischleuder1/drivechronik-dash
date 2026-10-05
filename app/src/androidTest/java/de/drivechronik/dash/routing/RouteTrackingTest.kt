package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RouteTrackingTest {

    private val points = (0..5).map {
        RoutePoint(52.0, 8.0 + it * 0.001)
    }

    private val route = OfflineRoute(
        points = points,
        distanceKm = 0.34,
        durationSeconds = 60.0,
        maneuvers = listOf(
            RouteManeuver(
                "Rechts abbiegen", 10, 0.1, 15.0, 3, 4
            )
        )
    )

    @Test
    fun normalProgress() {
        val tracker = RouteTracking()

        val a = tracker.update(route, points[0], 1.0)
        val b = tracker.update(route, points[1], 5.0)

        assertNotNull(a)
        assertNotNull(b)
        assertTrue(
            b!!.distanceAlongRouteMeters >
                a!!.distanceAlongRouteMeters
        )
        assertTrue(b.distanceToManeuverMeters!! > 0.0)
    }

    @Test
    fun rejectGpsJumps() {
        val tracker = RouteTracking()

        assertNotNull(tracker.update(route, points[0], 1.0))
        assertNull(tracker.update(route, points[5], 0.1))

        assertNotNull(tracker.update(route, points[2], 10.0))
        assertNull(tracker.update(route, points[0], 1.0))
    }

    @Test
    fun rejectOffRoute() {
        val tracker = RouteTracking()

        assertNull(
            tracker.update(
                route,
                RoutePoint(52.01, 8.01),
                1.0
            )
        )
    }
}
