package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaRoutableMessageCodecTest {

    @Test
    fun decodesOfficialTeslaProtocolExample() {

        /*
         * Beispiel aus Teslas offizieller
         * protocol.md Dokumentation.
         */
        val bytes =
            hex(
                "32020802" +
                "3a1212100a7962c10d38b61dd2a7722780a4f096" +
                "9a031005514f57616bcc81a8ce0f9d7b483229" +
                "52040a020805"
            )

        val message =
            TeslaRoutableMessageCodec
                .decode(bytes)

        assertEquals(
            UniversalMessage.Domain
                .DOMAIN_VEHICLE_SECURITY,
            message
                .getToDestination()
                .getDomain()
        )

        assertArrayEquals(
            hex(
                "0a7962c10d38b61dd2a7722780a4f096"
            ),
            message
                .getFromDestination()
                .getRoutingAddress()
                .toByteArray()
        )

        assertArrayEquals(
            hex(
                "0a020805"
            ),
            message
                .getProtobufMessageAsBytes()
                .toByteArray()
        )

        assertArrayEquals(
            hex(
                "05514f57616bcc81a8ce0f9d7b483229"
            ),
            message
                .getUuid()
                .toByteArray()
        )
    }


    @Test
    fun encodeDecodeRoundTripPreservesMessage() {

        val original =
            UniversalMessage
                .RoutableMessage
                .newBuilder()
                .setToDestination(
                    UniversalMessage
                        .Destination
                        .newBuilder()
                        .setDomain(
                            UniversalMessage
                                .Domain
                                .DOMAIN_INFOTAINMENT
                        )
                )
                .setFlags(1)
                .build()

        val encoded =
            TeslaRoutableMessageCodec
                .encode(original)

        val decoded =
            TeslaRoutableMessageCodec
                .decode(encoded)

        assertEquals(
            original,
            decoded
        )
    }


    private fun hex(
        value: String
    ): ByteArray {

        require(
            value.length % 2 == 0
        )

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
