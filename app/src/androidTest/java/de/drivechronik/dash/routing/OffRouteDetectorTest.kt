package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class OffRouteDetectorTest {
    @Test
    fun detectsPersistentDeviation() {
        val route = OfflineRoute(
            listOf(
                RoutePoint(52.0, 8.0),
                RoutePoint(52.0, 8.01)
            ),
            0.68, 60.0, emptyList()
        )
        val detector = OffRouteDetector()
        val near = RoutePoint(52.0, 8.005)
        val far = RoutePoint(52.002, 8.005)

        assertFalse(detector.update(route, far, 1000L))
        assertFalse(detector.update(route, near, 2000L))
        assertFalse(detector.update(route, far, 3000L))
        assertFalse(detector.update(route, far, 5000L))
        assertTrue(detector.update(route, far, 9000L))
    }
}
