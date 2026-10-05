package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RouteCrossingTest {

    @Test
    fun crossingKeepsRouteProgress() {
        val points = listOf(
            RoutePoint(52.000, 8.000),
            RoutePoint(52.001, 8.001),
            RoutePoint(52.002, 8.002),
            RoutePoint(52.001, 8.003),
            RoutePoint(52.000, 8.002),
            RoutePoint(52.001, 8.001),
            RoutePoint(52.002, 8.000)
        )

        val route = OfflineRoute(
            points = points,
            distanceKm = 1.0,
            durationSeconds = 120.0,
            maneuvers = emptyList()
        )

        val tracker = RouteTracking()

        for (i in 0..4) {
            assertNotNull(
                "Position $i abgewiesen",
                tracker.update(route, points[i], 30.0)
            )
        }

        // Zweite Durchfahrt derselben Kreuzung:
        // Der Fortschritt muss weiterlaufen.
        val result = tracker.update(
            route, points[5], 30.0
        )

        assertNotNull(
            "Kreuzung nicht zugeordnet", result
        )
        assertTrue(
            "Falscher Routenabschnitt",
            result!!.segmentIndex >= 4
        )
    }
}
