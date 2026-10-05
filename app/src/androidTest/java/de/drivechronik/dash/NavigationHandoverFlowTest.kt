package de.drivechronik.dash

import de.drivechronik.dash.routing.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class NavigationHandoverFlowTest {
    @Test
    fun switchDoesNotTriggerReroute() = runBlocking {
        val start = RoutePoint(52.0, 8.0)
        val end = RoutePoint(52.0, 8.01)
        val route = OfflineRoute(
            listOf(start, end), 0.68,
            60.0, emptyList(), end
        )

        val coordinator = RerouteCoordinator(route) { _, _ ->
            Result.failure(Exception("Test"))
        }
        val handover = NavigationGpsHandover()

        fun fix(source: String, lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.005),
                t * 1_000_000_000L,
                source, null
            )

        val initial = fix("tesla", 52.0, 1)
        assertTrue(handover.accept(initial))
        coordinator.onFix(initial.point, 1000)

        val first = fix("phone", 52.0001, 2)
        assertFalse(handover.accept(first))

        // Wiederholte identische Messung bleibt gesperrt.
        assertFalse(handover.accept(first))
        assertEquals(0, coordinator.attempts)

        val second = fix("phone", 52.0002, 3)
        assertTrue(handover.accept(second))

        coordinator.resetDetection()
        coordinator.onFix(second.point, 3000)

        assertEquals(0, coordinator.attempts)
        assertSame(route, coordinator.route)
    }
}
