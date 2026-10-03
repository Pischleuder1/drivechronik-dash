package de.drivechronik.dash.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaBleVehicleIdentityTest {

    @Test
    fun calculatesOfficialTeslaBleNameExample() {

        assertEquals(
            "S1a87a5a75f3df858C",

            TeslaBleVehicleIdentity
                .advertisementLocalName(
                    "5YJS0000000000000"
                )
        )
    }


    @Test
    fun normalizesVin() {

        assertEquals(
            "5YJS0000000000000",

            TeslaBleVehicleIdentity
                .normalizeVin(
                    " 5yjs0000000000000 "
                )
        )
    }


    @Test
    fun matchesExpectedVehicle() {

        assertTrue(
            TeslaBleVehicleIdentity
                .matches(
                    "5YJS0000000000000",
                    "S1a87a5a75f3df858C"
                )
        )


        assertFalse(
            TeslaBleVehicleIdentity
                .matches(
                    "5YJS0000000000000",
                    "S0000000000000000C"
                )
        )
    }


    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun rejectsInvalidVinLength() {

        TeslaBleVehicleIdentity
            .advertisementLocalName(
                "123"
            )
    }
}
