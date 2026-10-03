package de.drivechronik.dash.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaLiveProtocolSettingsTest {

    @Test
    fun defaultsAreSafe() {

        val config =
            TeslaLiveProtocolConfig()


        assertFalse(
            config.canStartSession
        )
    }


    @Test
    fun requiresAllActivationConditions() {

        val vin =
            "5YJ30123456789ABC"


        assertFalse(
            TeslaLiveProtocolConfig(
                enabled = true,
                keyEnrollmentConfirmed = false,
                vin = vin
            ).canStartSession
        )


        assertFalse(
            TeslaLiveProtocolConfig(
                enabled = false,
                keyEnrollmentConfirmed = true,
                vin = vin
            ).canStartSession
        )


        assertFalse(
            TeslaLiveProtocolConfig(
                enabled = true,
                keyEnrollmentConfirmed = true,
                vin = "TOO-SHORT"
            ).canStartSession
        )


        assertTrue(
            TeslaLiveProtocolConfig(
                enabled = true,
                keyEnrollmentConfirmed = true,
                vin = vin
            ).canStartSession
        )
    }


    @Test
    fun normalizesVin() {

        val config =
            TeslaLiveProtocolConfig(
                enabled = true,
                keyEnrollmentConfirmed = true,
                vin =
                    " 5yj30123456789abc "
            )


        assertEquals(
            "5YJ30123456789ABC",
            config.normalizedVin
        )

        assertTrue(
            config.canStartSession
        )
    }
}
