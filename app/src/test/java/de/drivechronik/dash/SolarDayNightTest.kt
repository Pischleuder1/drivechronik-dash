package de.drivechronik.dash

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SolarDayNightTest {

    private val zone =
        ZoneId.of(
            "Europe/Berlin"
        )

    private val latitude =
        52.52

    private val longitude =
        13.405


    @Test
    fun summerMiddayIsDay() {

        val time =
            ZonedDateTime.of(
                2026,
                6,
                21,
                12,
                0,
                0,
                0,
                zone
            )

        assertFalse(
            SolarDayNight.isNight(
                time,
                latitude,
                longitude
            )
        )
    }


    @Test
    fun summerLateEveningIsNight() {

        val time =
            ZonedDateTime.of(
                2026,
                6,
                21,
                23,
                30,
                0,
                0,
                zone
            )

        assertTrue(
            SolarDayNight.isNight(
                time,
                latitude,
                longitude
            )
        )
    }


    @Test
    fun winterEarlyMorningIsNight() {

        val time =
            ZonedDateTime.of(
                2026,
                12,
                21,
                5,
                0,
                0,
                0,
                zone
            )

        assertTrue(
            SolarDayNight.isNight(
                time,
                latitude,
                longitude
            )
        )
    }


    @Test
    fun winterMorningIsDay() {

        val time =
            ZonedDateTime.of(
                2026,
                12,
                21,
                9,
                0,
                0,
                0,
                zone
            )

        assertFalse(
            SolarDayNight.isNight(
                time,
                latitude,
                longitude
            )
        )
    }
}
