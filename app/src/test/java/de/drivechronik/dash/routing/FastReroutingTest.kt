package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class FastReroutingTest {

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
    fun detectsSideStreetWithinTwoSeconds() {
        val detector = OffRouteDetector()

        val offRoute = RoutePoint(52.0005, 8.0005)

        assertFalse(detector.update(route, offRoute, 1000))
        assertFalse(detector.update(
            route, RoutePoint(52.0005, 8.0006), 2000
        ))
        assertTrue(detector.update(
            route, RoutePoint(52.0005, 8.0007), 3000
        ))
        assertTrue(detector.confirmed)
    }

    @Test
    fun ignoresSmallGpsDeviations() {
        val detector = OffRouteDetector()

        val nearRoute = RoutePoint(52.0005, 8.0001)

        repeat(10) {
            assertFalse(
                detector.update(route, nearRoute, it * 1000L)
            )
        }
    }

    @Test
    fun resetsWhenReturningToRoute() {
        val detector = OffRouteDetector()
        val offRoute = RoutePoint(52.0005, 8.0005)
        val onRoute = RoutePoint(52.0005, 8.0000)

        detector.update(route, offRoute, 1000)
        detector.update(
            route, RoutePoint(52.0005, 8.0006), 2000
        )
        detector.update(
            route, RoutePoint(52.0005, 8.0007), 3000
        )

        assertTrue(detector.confirmed)
        assertFalse(detector.update(route, onRoute, 4000))
        assertFalse(detector.confirmed)
    }

    @Test
    fun stationaryGpsDeviationDoesNotTriggerRerouting() {
        val detector = OffRouteDetector()
        val offRoute = RoutePoint(52.0005, 8.0005)

        assertFalse(detector.update(route, offRoute, 1000))
        assertFalse(detector.update(route, offRoute, 2000))
        assertFalse(detector.update(route, offRoute, 3000))
        assertFalse(detector.confirmed)
    }

    @Test
    fun rerouteCooldownIsFiveSeconds() {
        val cooldown = RerouteCooldown()

        assertTrue(cooldown.canAttempt(1000))
        cooldown.markAttempt(1000)

        assertFalse(cooldown.canAttempt(5999))
        assertTrue(cooldown.canAttempt(6000))
    }
}
