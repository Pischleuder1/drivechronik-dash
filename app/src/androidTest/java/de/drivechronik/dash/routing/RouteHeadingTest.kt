package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RouteHeadingTest {
    @Test
    fun headingMatchesRoadDirection() {
        val route = OfflineRoute(
            points = listOf(
                RoutePoint(52.0, 8.0),
                RoutePoint(52.0, 8.01)
            ),
            distanceKm = 0.68,
            durationSeconds = 60.0,
            maneuvers = emptyList()
        )
        val position = RoutePoint(52.0, 8.005)

        // Richtung Osten: erlaubt
        assertNotNull(
            calculateRouteProgress(route, position,
                headingDegrees = 90f)
        )

        // Richtung Westen: falsche Fahrtrichtung
        assertNull(
            calculateRouteProgress(route, position,
                headingDegrees = 270f)
        )

        // Ohne Kurs: bisheriges Verhalten
        assertNotNull(
            calculateRouteProgress(route, position)
        )
    }
}
