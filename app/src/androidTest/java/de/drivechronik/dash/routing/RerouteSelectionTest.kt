package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RerouteSelectionTest {
    @Test
    fun successAndFailure() {
        val start = RoutePoint(52.28, 8.68)
        val end = RoutePoint(48.137, 11.575)

        val old = OfflineRoute(
            listOf(start, end), 636.0, 21000.0,
            emptyList(), end
        )
        val updated = old.copy(
            points = listOf(
                RoutePoint(52.29, 8.70), end
            ),
            distanceKm = 630.0
        )

        assertSame(
            old,
            selectReroutedRoute(
                old, Result.failure(Exception("Kein Weg"))
            )
        )
        assertSame(
            updated,
            selectReroutedRoute(
                old, Result.success(updated)
            )
        )
        assertSame(
            old,
            selectReroutedRoute(
                old, Result.success(
                    updated.copy(destination = start)
                )
            )
        )
    }
}
