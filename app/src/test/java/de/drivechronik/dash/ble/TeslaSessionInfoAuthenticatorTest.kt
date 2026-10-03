package de.drivechronik.dash.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaSessionInfoAuthenticatorTest {

    private val vin =
        "5YJ30123456789ABC"

    private val challenge =
        hex(
            "1588d5a30eabc6f8fc9a951b11f6fd11"
        )

    private val sharedKey =
        hex(
            "1b2fce19967b79db696f909cff89ea9a"
        )

    private val sessionInfo =
        hex(
            "0806124104" +
                "c7a1f47138486aa4729971494878d33b" +
                "1a24e39571f748a6e16c5955b3d877d3" +
                "a6aaa0e955166474af5d32c410f439a2" +
                "234137ad1bb085fd4e8813c958f11d97" +
                "1a10" +
                "4c463f9cc0d3d26906e982ed224adde6" +
                "25" +
                "5a0a0000"
        )


    @Test
    fun serializesOfficialTeslaHandshakeMetadata() {

        val metadata =
            TeslaSessionInfoAuthenticator
                .metadata(
                    vin = vin,
                    challenge = challenge
                )

        assertArrayEquals(
            hex(
                "000106" +
                    "0211" +
                    "35594a3330313233343536373839414243" +
                    "0610" +
                    "1588d5a30eabc6f8fc9a951b11f6fd11" +
                    "ff"
            ),
            metadata
        )
    }


    @Test
    fun calculatesOfficialTeslaSessionInfoTag() {

        val tag =
            TeslaSessionInfoAuthenticator
                .expectedTag(
                    sharedKey = sharedKey,
                    vin = vin,
                    challenge = challenge,
                    sessionInfoBytes =
                        sessionInfo
                )

        assertArrayEquals(
            hex(
                "996c1fe38331be138f8039c194b14db2" +
                    "198846ed7d8251e6749284d7b32ea002"
            ),
            tag
        )
    }


    @Test
    fun verifiesOfficialTeslaSessionInfoTag() {

        val tag =
            hex(
                "996c1fe38331be138f8039c194b14db2" +
                    "198846ed7d8251e6749284d7b32ea002"
            )

        assertTrue(
            TeslaSessionInfoAuthenticator
                .verifyTag(
                    sharedKey = sharedKey,
                    vin = vin,
                    challenge = challenge,
                    sessionInfoBytes =
                        sessionInfo,
                    receivedTag = tag
                )
        )
    }


    @Test
    fun rejectsModifiedSessionInfo() {

        val modified =
            sessionInfo.copyOf()

        modified[
            modified.lastIndex
        ] =
            (
                modified.last().toInt() xor 0x01
            ).toByte()

        val tag =
            hex(
                "996c1fe38331be138f8039c194b14db2" +
                    "198846ed7d8251e6749284d7b32ea002"
            )

        assertFalse(
            TeslaSessionInfoAuthenticator
                .verifyTag(
                    sharedKey = sharedKey,
                    vin = vin,
                    challenge = challenge,
                    sessionInfoBytes =
                        modified,
                    receivedTag = tag
                )
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
