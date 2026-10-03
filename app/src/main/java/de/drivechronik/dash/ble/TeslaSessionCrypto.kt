package de.drivechronik.dash.ble

import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.AlgorithmParameters
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.security.spec.ECParameterSpec
import java.security.spec.ECPoint
import java.security.spec.ECPublicKeySpec
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TeslaSessionCrypto {

    fun decodePublicKey(
        encoded: ByteArray
    ): ECPublicKey {

        require(encoded.size == 65) {
            "Tesla P256 public key must be 65 bytes"
        }

        require(
            encoded[0].toInt() and 0xff ==
                0x04
        ) {
            "Tesla P256 public key must be uncompressed"
        }

        val x =
            BigInteger(
                1,
                encoded.copyOfRange(
                    1,
                    33
                )
            )

        val y =
            BigInteger(
                1,
                encoded.copyOfRange(
                    33,
                    65
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

        val spec =
            ECPublicKeySpec(
                ECPoint(x, y),
                parameters
            )

        return KeyFactory
            .getInstance("EC")
            .generatePublic(spec)
            as ECPublicKey
    }


    fun deriveSharedKey(
        privateKey: PrivateKey,
        vehiclePublicKey:
            ECPublicKey
    ): ByteArray {

        val agreement =
            KeyAgreement
                .getInstance("ECDH")

        agreement.init(
            privateKey
        )

        agreement.doPhase(
            vehiclePublicKey,
            true
        )

        val secret =
            normalizeCoordinate(
                agreement.generateSecret()
            )

        val digest =
            MessageDigest
                .getInstance("SHA-1")
                .digest(secret)

        return digest.copyOfRange(
            0,
            16
        )
    }


    fun deriveSessionInfoKey(
        sharedKey: ByteArray
    ): ByteArray {

        require(
            sharedKey.size == 16
        ) {
            "Tesla shared key must be 16 bytes"
        }

        val mac =
            Mac.getInstance(
                "HmacSHA256"
            )

        mac.init(
            SecretKeySpec(
                sharedKey,
                "HmacSHA256"
            )
        )

        return mac.doFinal(
            "session info"
                .toByteArray(
                    StandardCharsets.US_ASCII
                )
        )
    }


    private fun normalizeCoordinate(
        value: ByteArray
    ): ByteArray {

        if (value.size == 32) {
            return value
        }

        val trimmed =
            value
                .dropWhile {
                    it == 0.toByte()
                }
                .toByteArray()

        require(
            trimmed.size <= 32
        ) {
            "ECDH shared coordinate exceeds 32 bytes"
        }

        return ByteArray(32).also {
            trimmed.copyInto(
                destination = it,
                destinationOffset =
                    32 - trimmed.size
            )
        }
    }
}
