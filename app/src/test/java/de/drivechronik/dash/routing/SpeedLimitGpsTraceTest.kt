package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class SpeedLimitGpsTraceTest {

    @Test
    fun requiresMovement() {
        val trace = SpeedLimitGpsTrace()

        trace.add(52.0000, 8.0000, 1000)
        assertTrue(trace.coordinates().isEmpty())

        trace.add(52.0001, 8.0000, 2000)
        assertTrue(trace.coordinates().isEmpty())

        trace.add(52.0002, 8.0000, 3000)
        assertEquals(3, trace.coordinates().size)
    }

    @Test
    fun ignoresDuplicatePosition() {
        val trace = SpeedLimitGpsTrace()

        assertTrue(trace.add(52.0, 8.0, 1000))
        assertFalse(trace.add(52.0, 8.0, 2000))
    }

    @Test
    fun resetsAfterGpsInterruption() {
        val trace = SpeedLimitGpsTrace()

        trace.add(52.0, 8.0, 1000)
        trace.add(52.0002, 8.0, 2000)

        trace.add(52.0004, 8.0, 14000)

        assertTrue(trace.coordinates().isEmpty())
    }

    @Test
    fun rejectsInvalidCoordinates() {
        val trace = SpeedLimitGpsTrace()

        assertFalse(trace.add(95.0, 8.0, 1000))
        assertTrue(trace.coordinates().isEmpty())
    }

    @Test
    fun limitsHistoryToSixSamples() {
        val trace = SpeedLimitGpsTrace()

        repeat(8) { i ->
            trace.add(
                52.0 + i * 0.0001,
                8.0,
                1000L + i * 1000L
            )
        }

        assertEquals(6, trace.coordinates().size)
    }
}
