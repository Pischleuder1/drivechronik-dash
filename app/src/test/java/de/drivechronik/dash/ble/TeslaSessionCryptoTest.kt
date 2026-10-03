package de.drivechronik.dash.ble

import java.math.BigInteger
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPrivateKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaSessionCryptoTest {

    @Test
    fun decodesOfficialTeslaSessionInfo() {

        val bytes =
            hex(
                "0806" +
                "124104" +
                "c7a1f47138486aa4729971494878d33b" +
                "1a24e39571f748a6e16c5955b3d877d3" +
                "a6aaa0e955166474af5d32c410f439a2" +
                "234137ad1bb085fd4e8813c958f11d97" +
                "1a10" +
                "4c463f9cc0d3d26906e982ed224adde6" +
                "25" +
                "5a0a0000"
            )

        val info =
            TeslaSessionInfoCodec
                .decode(bytes)

        assertEquals(
            6,
            info.counter
        )

        assertEquals(
            65,
            info.publicKey.size()
        )

        assertArrayEquals(
            hex(
                "4c463f9cc0d3d26906e982ed224adde6"
            ),
            info.epoch.toByteArray()
        )

        assertEquals(
            2650,
            info.clockTime
        )
    }


    @Test
    fun derivesOfficialTeslaSharedKey() {

        val clientPrivateScalar =
            BigInteger(
                "2538CDC29A97C19C1E99A637D6CF4F8C" +
                "970C118B56EDE1E6323E6D162C4B30DB",
                16
            )

        val vehiclePublicKey =
            TeslaSessionCrypto
                .decodePublicKey(
                    hex(
                        "04" +
                        "c7a1f47138486aa4729971494878d33b" +
                        "1a24e39571f748a6e16c5955b3d877d3" +
                        "a6aaa0e955166474af5d32c410f439a2" +
                        "234137ad1bb085fd4e8813c958f11d97"
                    )
                )

        val parameters =
            AlgorithmParameters
                .getInstance("EC")
                .apply {
                    init(
                        ECGenParameterSpec(
                            "secp256r1"
                        )
                    )
                }
                .getParameterSpec(
                    ECParameterSpec::class.java
                )

        val privateKey =
            KeyFactory
                .getInstance("EC")
                .generatePrivate(
                    ECPrivateKeySpec(
                        clientPrivateScalar,
                        parameters
                    )
                )

        val sharedKey =
            TeslaSessionCrypto
                .deriveSharedKey(
                    privateKey,
                    vehiclePublicKey
                )

        assertArrayEquals(
            hex(
                "1b2fce19967b79db696f909cff89ea9a"
            ),
            sharedKey
        )

        val sessionInfoKey =
            TeslaSessionCrypto
                .deriveSessionInfoKey(
                    sharedKey
                )

        assertArrayEquals(
            hex(
                "fceb679ee7bca756fcd441bf238bf2f" +
                "338629b41d9eb9c67be1b32c9672ce300"
            ),
            sessionInfoKey
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
