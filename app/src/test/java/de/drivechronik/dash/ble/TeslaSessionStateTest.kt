package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaSessionStateTest {

    private val epochA =
        hex(
            "4c463f9cc0d3d26906e982ed224adde6"
        )

    private val epochB =
        hex(
            "0102030405060708090a0b0c0d0e0f10"
        )

    private val sharedKey =
        hex(
            "1b2fce19967b79db696f909cff89ea9a"
        )


    @Test
    fun createsStateFromOfficialTeslaValues() {

        val state =
            TeslaSessionState
                .fromAuthenticatedSessionInfo(
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    info =
                        sessionInfo(
                            epoch = epochA,
                            counter = 6,
                            clock = 2650
                        ),
                    sharedKey =
                        sharedKey,
                    receivedAtNanos =
                        1_000_000_000L
                )

        assertEquals(
            6L,
            state.currentCounter()
        )

        assertArrayEquals(
            epochA,
            state.epoch()
        )

        assertArrayEquals(
            sharedKey,
            state.sharedKey()
        )
    }


    @Test
    fun firstCommandUsesNextCounter() {

        val state =
            createState()

        assertEquals(
            7L,
            state.reserveCounter()
        )

        assertEquals(
            7L,
            state.currentCounter()
        )
    }


    @Test
    fun advancesVehicleClockUsingMonotonicTime() {

        val state =
            TeslaSessionState
                .fromAuthenticatedSessionInfo(
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    info =
                        sessionInfo(
                            epoch = epochA,
                            counter = 6,
                            clock = 2650
                        ),
                    sharedKey =
                        sharedKey,
                    receivedAtNanos =
                        1_000_000_000L
                )

        assertEquals(
            2655L,
            state.vehicleTimeSeconds(
                6_000_000_000L
            )
        )
    }


    @Test
    fun neverRollsBackCounterInsideSameEpoch() {

        val state =
            createState()

        /*
         * Counter 7 wurde bereits für einen
         * Request reserviert.
         */
        assertEquals(
            7L,
            state.reserveCounter()
        )

        val accepted =
            state.updateAuthenticatedSessionInfo(
                info =
                    sessionInfo(
                        epoch = epochA,
                        counter = 4,
                        clock = 2651
                    ),
                newSharedKey =
                    sharedKey,
                receivedAtNanos =
                    2_000_000_000L
            )

        assertTrue(
            accepted
        )

        assertEquals(
            7L,
            state.currentCounter()
        )
    }


    @Test
    fun rejectsOlderClockInsideSameEpoch() {

        val state =
            createState()

        val accepted =
            state.updateAuthenticatedSessionInfo(
                info =
                    sessionInfo(
                        epoch = epochA,
                        counter = 9,
                        clock = 2649
                    ),
                newSharedKey =
                    sharedKey,
                receivedAtNanos =
                    2_000_000_000L
            )

        assertFalse(
            accepted
        )

        assertEquals(
            6L,
            state.currentCounter()
        )

        assertArrayEquals(
            epochA,
            state.epoch()
        )
    }


    @Test
    fun newEpochMayResetCounter() {

        val state =
            createState()

        assertEquals(
            7L,
            state.reserveCounter()
        )

        val accepted =
            state.updateAuthenticatedSessionInfo(
                info =
                    sessionInfo(
                        epoch = epochB,
                        counter = 2,
                        clock = 10
                    ),
                newSharedKey =
                    sharedKey,
                receivedAtNanos =
                    2_000_000_000L
            )

        assertTrue(
            accepted
        )

        assertArrayEquals(
            epochB,
            state.epoch()
        )

        assertEquals(
            2L,
            state.currentCounter()
        )

        assertEquals(
            3L,
            state.reserveCounter()
        )
    }


    private fun createState():
        TeslaSessionState {

        return TeslaSessionState
            .fromAuthenticatedSessionInfo(
                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,
                info =
                    sessionInfo(
                        epoch = epochA,
                        counter = 6,
                        clock = 2650
                    ),
                sharedKey =
                    sharedKey,
                receivedAtNanos =
                    1_000_000_000L
            )
    }


    private fun sessionInfo(
        epoch: ByteArray,
        counter: Int,
        clock: Int
    ): Signatures.SessionInfo {

        return Signatures
            .SessionInfo
            .newBuilder()
            .setCounter(
                counter
            )
            .setEpoch(
                ByteString.copyFrom(
                    epoch
                )
            )
            .setClockTime(
                clock
            )
            .build()
    }


    private fun hex(
        value: String
    ): ByteArray {

        return ByteArray(
            value.length / 2
        ) { index ->

            value
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}
