package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaEncryptedCommandBuilderTest {

    @Test
    fun buildsOfficialTeslaEncryptedRoutableMessage() {

        val clientPublicKey =
            hex(
                "04" +
                "b2b6bc68c2da0665ce656815594996c6" +
                "2394edd8bea905fe781a754fe6a845a7" +
                "14330902f225e9269d466e05b349981f" +
                "da9d85cc23c6fb444aa73b629105dc6e"
            )

        val result =
            TeslaEncryptedCommandBuilder
                .build(
                    plaintext =
                        hex(
                            "120452020801"
                        ),
                    clientPublicKey =
                        clientPublicKey,
                    sharedKey =
                        hex(
                            "1b2fce19967b79db696f909cff89ea9a"
                        ),
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    epoch =
                        hex(
                            "4c463f9cc0d3d26906e982ed224adde6"
                        ),
                    routingAddress =
                        hex(
                            "2c907bd76c640d360b3027dc7404efde"
                        ),
                    uuid =
                        hex(
                            "58406580528b6a5301391800b4fe9b99"
                        ),
                    expiresAt = 2655,
                    counter = 7,
                    flags = 2,
                    nonce =
                        hex(
                            "dbf79447fa156674dae1caed"
                        )
                )

        val message =
            result.message


        assertEquals(
            UniversalMessage
                .Domain
                .DOMAIN_INFOTAINMENT,
            message
                .toDestination
                .domain
        )


        assertArrayEquals(
            hex(
                "2c907bd76c640d360b3027dc7404efde"
            ),
            message
                .fromDestination
                .routingAddress
                .toByteArray()
        )


        assertArrayEquals(
            hex(
                "38038e8c0f2e"
            ),
            message
                .protobufMessageAsBytes
                .toByteArray()
        )


        assertTrue(
            message.hasSignatureData()
        )


        assertArrayEquals(
            clientPublicKey,
            message
                .signatureData
                .signerIdentity
                .publicKey
                .toByteArray()
        )


        val aes =
            message
                .signatureData
                .aesgcmPersonalizedData


        assertArrayEquals(
            hex(
                "4c463f9cc0d3d26906e982ed224adde6"
            ),
            aes
                .epoch
                .toByteArray()
        )


        assertArrayEquals(
            hex(
                "dbf79447fa156674dae1caed"
            ),
            aes
                .nonce
                .toByteArray()
        )


        assertEquals(
            7,
            aes.counter
        )


        assertEquals(
            2655,
            aes.expiresAt
        )


        assertArrayEquals(
            hex(
                "c228e0ff64991481db3a7bbc133696c5"
            ),
            aes
                .tag
                .toByteArray()
        )


        assertEquals(
            2,
            message.flags
        )


        assertArrayEquals(
            hex(
                "58406580528b6a5301391800b4fe9b99"
            ),
            message
                .uuid
                .toByteArray()
        )


        /*
         * Request hash:
         *
         * 05 = AES_GCM_PERSONALIZED
         * danach der 16-Byte GCM-Tag.
         */
        assertArrayEquals(
            hex(
                "05" +
                "c228e0ff64991481db3a7bbc133696c5"
            ),
            result.requestHash
        )
    }


    @Test
    fun protobufRoundTripPreservesMessage() {

        val result =
            TeslaEncryptedCommandBuilder
                .build(
                    plaintext =
                        hex(
                            "120452020801"
                        ),
                    clientPublicKey =
                        hex(
                            "04" +
                            "b2b6bc68c2da0665ce656815594996c6" +
                            "2394edd8bea905fe781a754fe6a845a7" +
                            "14330902f225e9269d466e05b349981f" +
                            "da9d85cc23c6fb444aa73b629105dc6e"
                        ),
                    sharedKey =
                        hex(
                            "1b2fce19967b79db696f909cff89ea9a"
                        ),
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    epoch =
                        hex(
                            "4c463f9cc0d3d26906e982ed224adde6"
                        ),
                    routingAddress =
                        hex(
                            "2c907bd76c640d360b3027dc7404efde"
                        ),
                    uuid =
                        hex(
                            "58406580528b6a5301391800b4fe9b99"
                        ),
                    expiresAt = 2655,
                    counter = 7,
                    flags = 2,
                    nonce =
                        hex(
                            "dbf79447fa156674dae1caed"
                        )
                )

        val encoded =
            TeslaRoutableMessageCodec
                .encode(
                    result.message
                )

        val decoded =
            TeslaRoutableMessageCodec
                .decode(
                    encoded
                )

        assertEquals(
            result.message,
            decoded
        )
    }


    private fun hex(
        value: String
    ): ByteArray {

        val clean =
            value.replace(
                "\\s".toRegex(),
                ""
            )

        require(
            clean.length % 2 == 0
        )

        return ByteArray(
            clean.length / 2
        ) { index ->

            clean
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}
