package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test

class OfflineStartupGateTest {
    @Test
    fun startupStates() {
        OfflineStartupGate.begin()
        assertFalse(OfflineStartupGate.ready)
        assertNull(OfflineStartupGate.error)

        OfflineStartupGate.success()
        assertTrue(OfflineStartupGate.ready)
        assertNull(OfflineStartupGate.error)

        OfflineStartupGate.failure("Testfehler")
        assertFalse(OfflineStartupGate.ready)
        assertEquals("Testfehler", OfflineStartupGate.error)

        OfflineStartupGate.begin()
        assertFalse(OfflineStartupGate.ready)
        assertNull(OfflineStartupGate.error)
    }

    @Test
    fun independentMapAndRouting() {
        try {
            OfflineStartupGate.begin()
            assertFalse(OfflineStartupGate.mapReady)
            assertFalse(OfflineStartupGate.routingReady)

            OfflineStartupGate.mapFailure("PMTiles beschädigt")
            OfflineStartupGate.routingSuccess()

            assertFalse(OfflineStartupGate.mapReady)
            assertTrue(OfflineStartupGate.routingReady)
            assertEquals(
                "PMTiles beschädigt",
                OfflineStartupGate.mapError
            )

            OfflineStartupGate.begin()
            OfflineStartupGate.mapSuccess()
            OfflineStartupGate.routingFailure("Valhalla beschädigt")

            assertTrue(OfflineStartupGate.mapReady)
            assertFalse(OfflineStartupGate.routingReady)
            assertEquals(
                "Valhalla beschädigt",
                OfflineStartupGate.routingError
            )
        } finally {
            OfflineStartupGate.begin()
        }
    }

}
