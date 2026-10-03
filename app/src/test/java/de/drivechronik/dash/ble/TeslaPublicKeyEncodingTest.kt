package de.drivechronik.dash.ble

import java.math.BigInteger
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaPublicKeyEncodingTest {

    @Test
    fun encodesOfficialTeslaClientPublicKey() {

        val x =
            BigInteger(
                "B2B6BC68C2DA0665CE656815594996C6" +
                "2394EDD8BEA905FE781A754FE6A845A7",
                16
            )

        val y =
            BigInteger(
                "14330902F225E9269D466E05B349981F" +
                "DA9D85CC23C6FB444AA73B629105DC6E",
                16
            )

        val encoded =
            TeslaPublicKeyEncoding
                .encodeCoordinates(
                    x,
                    y
                )

        val expected =
            hex(
                "04" +
                "b2b6bc68c2da0665ce656815594996c6" +
                "2394edd8bea905fe781a754fe6a845a7" +
                "14330902f225e9269d466e05b349981f" +
                "da9d85cc23c6fb444aa73b629105dc6e"
            )

        assertEquals(
            65,
            encoded.size
        )

        assertArrayEquals(
            expected,
            encoded
        )
    }

    private fun hex(
        value: String
    ): ByteArray {

        return ByteArray(
            value.length / 2
        ) { i ->

            value
                .substring(
                    i * 2,
                    i * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}
