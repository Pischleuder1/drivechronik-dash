package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object TeslaSessionInfoAuthenticator {

    fun metadata(
        vin: String,
        challenge: ByteArray
    ): ByteArray {

        require(
            vin.length == 17
        ) {
            "VIN must contain exactly 17 characters"
        }

        require(
            challenge.isNotEmpty() &&
                challenge.size <= 16
        ) {
            "Handshake challenge must contain 1..16 bytes"
        }

        return TeslaMetadataSerializer
            .serialize(
                listOf(
                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_SIGNATURE_TYPE
                                .number,
                        value =
                            byteArrayOf(
                                Signatures
                                    .SignatureType
                                    .SIGNATURE_TYPE_HMAC
                                    .number
                                    .toByte()
                            )
                    ),

                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_PERSONALIZATION
                                .number,
                        value =
                            vin.toByteArray(
                                StandardCharsets.US_ASCII
                            )
                    ),

                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_CHALLENGE
                                .number,
                        value =
                            challenge.copyOf()
                    )
                )
            )
    }


    fun expectedTag(
        sharedKey: ByteArray,
        vin: String,
        challenge: ByteArray,
        sessionInfoBytes: ByteArray
    ): ByteArray {

        require(
            sharedKey.size == 16
        ) {
            "Tesla shared key must contain 16 bytes"
        }

        val sessionInfoKey =
            TeslaSessionCrypto
                .deriveSessionInfoKey(
                    sharedKey
                )

        val input =
            metadata(
                vin = vin,
                challenge = challenge
            ) + sessionInfoBytes

        val mac =
            Mac.getInstance(
                "HmacSHA256"
            )

        mac.init(
            SecretKeySpec(
                sessionInfoKey,
                "HmacSHA256"
            )
        )

        return mac.doFinal(
            input
        )
    }


    fun verifyTag(
        sharedKey: ByteArray,
        vin: String,
        challenge: ByteArray,
        sessionInfoBytes: ByteArray,
        receivedTag: ByteArray
    ): Boolean {

        val expected =
            expectedTag(
                sharedKey = sharedKey,
                vin = vin,
                challenge = challenge,
                sessionInfoBytes =
                    sessionInfoBytes
            )

        return MessageDigest.isEqual(
            expected,
            receivedTag
        )
    }
}
