package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test

class NavigationGpsAgeTest {
    @Test
    fun gpsAgeValidation() {
        val now = 20_000_000_000L

        assertTrue(navigationGpsIsFresh(now, now))
        assertTrue(navigationGpsIsFresh(
            now - 7_000_000_000L, now
        ))
        assertTrue(navigationGpsIsFresh(
            now - 8_000_000_000L, now
        ))

        assertFalse(navigationGpsIsFresh(
            now - 9_000_000_000L, now
        ))
        assertFalse(navigationGpsIsFresh(
            now + 1_000_000L, now
        ))
    }
}
