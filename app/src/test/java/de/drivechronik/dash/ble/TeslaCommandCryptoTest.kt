package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class TeslaCommandCryptoTest {

    @Test
    fun reproducesOfficialTeslaAesGcmExample() {

        val plaintext =
            hex(
                "120452020801"
            )

        val sharedKey =
            hex(
                "1b2fce19967b79db696f909cff89ea9a"
            )

        val epoch =
            hex(
                "4c463f9cc0d3d26906e982ed224adde6"
            )

        val nonce =
            hex(
                "dbf79447fa156674dae1caed"
            )

        val result =
            TeslaCommandCrypto
                .encrypt(
                    plaintext = plaintext,
                    sharedKey = sharedKey,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    epoch = epoch,
                    expiresAt = 2655,
                    counter = 7,
                    flags = 2,
                    nonce = nonce
                )

        assertArrayEquals(
            hex(
                "000105" +
                "010103" +
                "021135594a3330313233343536373839414243" +
                "03104c463f9cc0d3d26906e982ed224adde6" +
                "040400000a5f" +
                "050400000007" +
                "070400000002" +
                "ff"
            ),
            result.metadata
        )

        assertArrayEquals(
            hex(
                "54785b6878a51a70acaf240199f92f48" +
                "2962a73a8c3170bfd2a94b1715d127b9"
            ),
            result.aad
        )

        assertArrayEquals(
            hex(
                "38038e8c0f2e"
            ),
            result.ciphertext
        )

        assertArrayEquals(
            hex(
                "c228e0ff64991481db3a7bbc133696c5"
            ),
            result.tag
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
