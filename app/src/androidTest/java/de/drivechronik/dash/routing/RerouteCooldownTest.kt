package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class RerouteCooldownTest {
    @Test
    fun cooldownWorks() {
        val gate = RerouteCooldown()

        assertTrue(gate.canAttempt(100_000L))
        gate.markAttempt(100_000L)

        assertFalse(gate.canAttempt(101_000L))
        assertFalse(gate.canAttempt(159_999L))
        assertTrue(gate.canAttempt(160_000L))

        gate.markAttempt(160_000L)
        assertFalse(gate.canAttempt(150_000L))

        gate.reset()
        assertTrue(gate.canAttempt(150_000L))
    }
}
