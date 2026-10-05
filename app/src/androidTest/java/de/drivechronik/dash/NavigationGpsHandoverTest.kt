package de.drivechronik.dash

import de.drivechronik.dash.routing.RoutePoint
import org.junit.Assert.*
import org.junit.Test

class NavigationGpsHandoverTest {
    @Test
    fun sourceSwitchIsVerified() {
        val handover = NavigationGpsHandover()

        fun fix(
            lat: Double,
            source: String,
            seconds: Long
        ) = NavigationGpsFix(
            RoutePoint(lat, 8.0),
            seconds * 1_000_000_000L,
            source, null
        )

        assertTrue(handover.accept(
            fix(52.0, "tesla", 1)
        ))

        // Erste abweichende Position ablehnen.
        assertFalse(handover.accept(
            fix(52.0001, "phone", 2)
        ))

        // Zweite, konsistente Messung zulassen.
        assertTrue(handover.accept(
            fix(52.0002, "phone", 3)
        ))

        // Rueckwechsel ebenfalls bestaetigen.
        assertFalse(handover.accept(
            fix(52.0, "tesla", 4)
        ))
        assertTrue(handover.accept(
            fix(52.0001, "tesla", 5)
        ))
    }

    @Test
    fun returnToTeslaGps() {
        val h = NavigationGpsHandover()

        fun fix(src: String, t: Long, lat: Double) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L, src, null
            )

        assertTrue(h.accept(fix("phone", 1, 52.0)))
        assertFalse(h.accept(fix("tesla", 2, 52.0)))
        assertTrue(h.accept(fix("tesla", 3, 52.0001)))
        assertTrue(h.accept(fix("tesla", 4, 52.0002)))
    }

    @Test
    fun interruptionRequiresFreshHandover() {
        val h = NavigationGpsHandover()

        fun fix(src: String, t: Long) =
            NavigationGpsFix(
                RoutePoint(52.0, 8.0),
                t * 1_000_000_000L, src, null
            )

        assertTrue(h.accept(fix("tesla", 1)))
        assertFalse(h.accept(fix("phone", 2)))

        // Langer GPS-Ausfall: Zustand zurücksetzen.
        h.reset()

        assertTrue(h.accept(fix("phone", 30)))
        assertFalse(h.accept(fix("tesla", 31)))
        assertTrue(h.accept(fix("tesla", 32)))
    }

    @Test
    fun rejectLargeSourceJump() {
        val h = NavigationGpsHandover()

        fun fix(src: String, lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L,
                src, null
            )

        assertTrue(h.accept(fix("tesla", 52.0, 1)))

        // Smartphone meldet rund 330 Meter Versatz.
        assertFalse(h.accept(fix("phone", 52.003, 2)))
        assertFalse(h.accept(fix("phone", 52.0031, 3)))
    }

    @Test
    fun timeAwareSourceSwitch() {
        fun fix(src: String, lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L, src, null
            )

        val fastJump = NavigationGpsHandover()
        assertTrue(fastJump.accept(
            fix("tesla", 52.0, 1), 1_000_000_000L
        ))
        assertFalse(fastJump.accept(
            fix("phone", 52.003, 2), 2_000_000_000L
        ))
        assertFalse(fastJump.accept(
            fix("phone", 52.0031, 3), 3_000_000_000L
        ))

        val motorway = NavigationGpsHandover()
        assertTrue(motorway.accept(
            fix("tesla", 52.0, 1), 1_000_000_000L
        ))
        assertFalse(motorway.accept(
            fix("phone", 52.0017, 5), 5_000_000_000L
        ))
        assertTrue(motorway.accept(
            fix("phone", 52.0018, 6), 6_000_000_000L
        ))
    }

    @Test
    fun longGapRecovery() {
        val h = NavigationGpsHandover()

        fun fix(src: String, lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L, src, null
            )

        assertTrue(h.accept(fix("tesla", 52.0, 1),
            1_000_000_000L))
        assertFalse(h.accept(fix("phone", 52.001, 30),
            30_000_000_000L))
        assertTrue(h.accept(fix("phone", 52.0011, 31),
            31_000_000_000L))
    }

    @Test
    fun rejectSameSourceJump() {
        val h = NavigationGpsHandover()

        fun fix(lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L,
                "tesla", null
            )

        assertTrue(h.accept(fix(52.0, 1), 1_000_000_000L))
        assertFalse(h.accept(fix(52.005, 2), 2_000_000_000L))
    }

    @Test
    fun recoversAfterGpsOutlier() {
        val h = NavigationGpsHandover()

        fun fix(lat: Double, t: Long) =
            NavigationGpsFix(
                RoutePoint(lat, 8.0),
                t * 1_000_000_000L,
                "tesla", null
            )

        assertTrue(h.accept(fix(52.0, 1), 1_000_000_000L))

        // Unplausibler Sprung.
        assertFalse(h.accept(fix(52.005, 2), 2_000_000_000L))

        // Naechste plausible Position.
        assertTrue(h.accept(fix(52.0002, 3), 3_000_000_000L))
    }
}
