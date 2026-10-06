package de.drivechronik.dash.routing

import org.junit.Assert.*
import org.junit.Test

class ArrivalDetectorTest {

    private fun progress(
        remaining: Double,
        offRoute: Double = 2.0
    ) = RouteProgress(
        segmentIndex = 0,
        distanceAlongRouteMeters = 100.0,
        distanceRemainingMeters = remaining,
        distanceFromRouteMeters = offRoute,
        distanceToManeuverMeters = null,
        nextManeuver = null
    )

    @Test
    fun stopsAfterConfirmedArrival() {
        val detector = ArrivalDetector()
        assertFalse(detector.update(progress(10.0), 12.0, 1000))
        assertFalse(detector.update(progress(9.0), 11.0, 2000))
        assertTrue(detector.update(progress(8.0), 10.0, 3000))
    }

    @Test
    fun doesNotStopWhenPassingDestination() {
        val detector = ArrivalDetector()
        repeat(5) {
            assertFalse(
                detector.update(progress(150.0), 10.0, it * 1000L)
            )
        }
    }

    @Test
    fun gpsInterruptionResetsArrival() {
        val detector = ArrivalDetector()
        detector.update(progress(10.0), 10.0, 1000)
        detector.update(progress(10.0), 10.0, 2000)
        detector.reset()

        assertFalse(detector.update(progress(10.0), 10.0, 3000))
        assertFalse(detector.update(progress(10.0), 10.0, 4000))
        assertTrue(detector.update(progress(10.0), 10.0, 5000))
    }

    @Test
    fun rejectsOffRouteArrival() {
        val detector = ArrivalDetector()
        repeat(5) {
            assertFalse(
                detector.update(progress(10.0, 40.0), 10.0, it * 1000L)
            )
        }
    }
}
