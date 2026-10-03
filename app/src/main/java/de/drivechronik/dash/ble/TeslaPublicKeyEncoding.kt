package de.drivechronik.dash.ble

import java.math.BigInteger
import java.security.interfaces.ECPublicKey

object TeslaPublicKeyEncoding {

    const val ENCODED_SIZE = 65

    fun encode(
        publicKey: ECPublicKey
    ): ByteArray {

        return encodeCoordinates(
            publicKey.w.affineX,
            publicKey.w.affineY
        )
    }

    fun encodeCoordinates(
        x: BigInteger,
        y: BigInteger
    ): ByteArray {

        val result =
            ByteArray(ENCODED_SIZE)

        result[0] = 0x04

        x.toUnsignedFixed32()
            .copyInto(
                result,
                destinationOffset = 1
            )

        y.toUnsignedFixed32()
            .copyInto(
                result,
                destinationOffset = 33
            )

        return result
    }

    private fun BigInteger.toUnsignedFixed32():
        ByteArray {

        require(signum() >= 0) {
            "EC coordinate must be positive"
        }

        val raw = toByteArray()

        val unsigned =
            if (
                raw.size > 1 &&
                raw[0] == 0.toByte()
            ) {
                raw.copyOfRange(
                    1,
                    raw.size
                )
            }
            else {
                raw
            }

        require(unsigned.size <= 32) {
            "EC coordinate exceeds 32 bytes"
        }

        return ByteArray(32).also {
            unsigned.copyInto(
                destination = it,
                destinationOffset =
                    32 - unsigned.size
            )
        }
    }
}
