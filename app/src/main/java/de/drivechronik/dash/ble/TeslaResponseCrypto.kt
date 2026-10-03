package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object TeslaResponseCrypto {

    const val NONCE_SIZE = 12
    const val TAG_SIZE = 16

    data class DecryptedPayload(
        val metadata: ByteArray,
        val aad: ByteArray,
        val plaintext: ByteArray
    )


    fun metadata(
        domain: UniversalMessage.Domain,
        vin: String,
        counter: Long,
        flags: Long,
        requestHash: ByteArray,
        fault: Long
    ): ByteArray {

        require(vin.length == 17) {
            "VIN must contain exactly 17 characters"
        }

        require(counter in 0..0xffff_ffffL)
        require(flags in 0..0xffff_ffffL)
        require(fault in 0..0xffff_ffffL)

        require(
            requestHash.isNotEmpty() &&
                requestHash.size <= 33
        ) {
            "Request hash must contain 1..33 bytes"
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
                                    .SIGNATURE_TYPE_AES_GCM_RESPONSE
                                    .number
                                    .toByte()
                            )
                    ),

                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_DOMAIN
                                .number,
                        value =
                            byteArrayOf(
                                domain.number.toByte()
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
                                .TAG_COUNTER
                                .number,
                        value =
                            TeslaMetadataSerializer
                                .uint32(counter)
                    ),

                    /*
                     * Bei Antworten immer
                     * aufnehmen, auch wenn 0.
                     */
                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_FLAGS
                                .number,
                        value =
                            TeslaMetadataSerializer
                                .uint32(flags)
                    ),

                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_REQUEST_HASH
                                .number,
                        value =
                            requestHash.copyOf()
                    ),

                    TeslaMetadataSerializer.Item(
                        tag =
                            Signatures
                                .Tag
                                .TAG_FAULT
                                .number,
                        value =
                            TeslaMetadataSerializer
                                .uint32(fault)
                    )
                )
            )
    }


    fun decrypt(
        ciphertext: ByteArray,
        sharedKey: ByteArray,
        domain: UniversalMessage.Domain,
        vin: String,
        counter: Long,
        flags: Long,
        requestHash: ByteArray,
        fault: Long,
        nonce: ByteArray,
        tag: ByteArray
    ): DecryptedPayload {

        require(sharedKey.size == 16) {
            "Tesla AES key must contain 16 bytes"
        }

        require(nonce.size == NONCE_SIZE) {
            "AES-GCM nonce must contain 12 bytes"
        }

        require(tag.size == TAG_SIZE) {
            "AES-GCM tag must contain 16 bytes"
        }

        val metadata =
            metadata(
                domain = domain,
                vin = vin,
                counter = counter,
                flags = flags,
                requestHash = requestHash,
                fault = fault
            )

        val aad =
            MessageDigest
                .getInstance("SHA-256")
                .digest(metadata)

        val cipher =
            Cipher.getInstance(
                "AES/GCM/NoPadding"
            )

        cipher.init(
            Cipher.DECRYPT_MODE,
            SecretKeySpec(
                sharedKey,
                "AES"
            ),
            GCMParameterSpec(
                TAG_SIZE * 8,
                nonce
            )
        )

        cipher.updateAAD(aad)

        val plaintext =
            cipher.doFinal(
                ciphertext + tag
            )

        return DecryptedPayload(
            metadata = metadata,
            aad = aad,
            plaintext = plaintext
        )
    }
}
