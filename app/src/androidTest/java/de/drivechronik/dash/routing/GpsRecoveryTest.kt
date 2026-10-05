package de.drivechronik.dash.routing

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GpsRecoveryTest {
    @Test
    fun interruptedGpsResetsDetection() = runBlocking {
        val start = RoutePoint(52.0, 8.0)
        val end = RoutePoint(52.0, 8.01)
        val far = RoutePoint(52.002, 8.005)
        val near = RoutePoint(52.0, 8.005)

        val route = OfflineRoute(
            listOf(start, end), 0.68,
            60.0, emptyList(), end
        )

        val flow = RerouteCoordinator(route) { _, _ ->
            Result.failure(Exception("Test"))
        }

        // Zwei Messungen ausserhalb der Route.
        assertFalse(flow.onFix(far, 1000))
        assertFalse(flow.onFix(far, 3000))

        // GPS-Ausfall: keine Messungen.
        // Bei Wiederaufnahme wird die Erkennung
        // wie in der Navigation zurueckgesetzt.
        flow.resetDetection()

        // Wieder auf der Route.
        assertFalse(flow.onFix(near, 15000))
        assertEquals(0, flow.attempts)

        // Neue Abweichung muss erneut bestaetigt werden.
        assertFalse(flow.onFix(far, 20000))
        assertFalse(flow.onFix(far, 22000))
        assertFalse(flow.onFix(far, 26000))

        // Genau ein Neuberechnungsversuch.
        assertEquals(1, flow.attempts)

        // Bei Fehler bleibt die alte Route erhalten.
        assertSame(route, flow.route)
    }
}
