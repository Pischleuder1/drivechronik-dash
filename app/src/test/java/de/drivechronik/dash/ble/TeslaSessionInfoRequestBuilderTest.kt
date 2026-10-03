package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaSessionInfoRequestBuilderTest {

    @Test
    fun buildsTeslaHandshakeRequest() {

        val publicKey =
            ByteArray(65) {
                it.toByte()
            }.also {
                it[0] = 0x04
            }

        val routingAddress =
            ByteArray(16) {
                (it + 1).toByte()
            }

        val uuid =
            ByteArray(16) {
                (0x80 + it).toByte()
            }

        val message =
            TeslaSessionInfoRequestBuilder
                .build(
                    publicKey = publicKey,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    routingAddress =
                        routingAddress,
                    uuid = uuid
                )

        assertEquals(
            UniversalMessage
                .Domain
                .DOMAIN_INFOTAINMENT,
            message
                .toDestination
                .domain
        )

        assertArrayEquals(
            routingAddress,
            message
                .fromDestination
                .routingAddress
                .toByteArray()
        )

        assertTrue(
            message.hasSessionInfoRequest()
        )

        assertArrayEquals(
            publicKey,
            message
                .sessionInfoRequest
                .publicKey
                .toByteArray()
        )

        assertArrayEquals(
            uuid,
            message
                .uuid
                .toByteArray()
        )

        assertEquals(
            2,
            message.flags
        )
    }


    @Test
    fun randomRequestUsesCorrectSizes() {

        val publicKey =
            ByteArray(65).also {
                it[0] = 0x04
            }

        val message =
            TeslaSessionInfoRequestBuilder
                .buildRandom(
                    publicKey,
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT
                )

        assertEquals(
            16,
            message
                .fromDestination
                .routingAddress
                .size()
        )

        assertEquals(
            16,
            message
                .uuid
                .size()
        )
    }
}
