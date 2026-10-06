package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class SideStreetHeadingTest {

    private val route = OfflineRoute(
        points = listOf(
            RoutePoint(52.0000, 8.0000),
            RoutePoint(52.0010, 8.0000)
        ),
        distanceKm = 0.111,
        durationSeconds = 15.0,
        maneuvers = emptyList()
    )

    @Test
    fun wrongTurnDetectedBeforeThirtyMeters() {
        val detector = OffRouteDetector()

        assertFalse(detector.update(
            route, RoutePoint(52.0005, 8.00000), 1000, 90f
        ))
        assertFalse(detector.update(
            route, RoutePoint(52.0005, 8.00010), 2000, 90f
        ))
        assertTrue(detector.update(
            route, RoutePoint(52.0005, 8.00020), 3000, 90f
        ))
    }

    @Test
    fun correctDirectionDoesNotTrigger() {
        val detector = OffRouteDetector()

        repeat(5) {
            assertFalse(detector.update(
                route,
                RoutePoint(52.0001 + it * 0.0001, 8.0000),
                it * 1000L,
                0f
            ))
        }
    }

    @Test
    fun stationaryHeadingErrorDoesNotTrigger() {
        val detector = OffRouteDetector()

        repeat(5) {
            assertFalse(detector.update(
                route,
                RoutePoint(52.0005, 8.0000),
                it * 1000L,
                90f
            ))
        }
    }
}
