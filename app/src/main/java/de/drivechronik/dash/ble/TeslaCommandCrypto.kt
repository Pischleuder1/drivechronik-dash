package de.drivechronik.dash.ble

import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object TeslaCommandCrypto {

    const val NONCE_SIZE = 12
    const val TAG_SIZE = 16

    data class EncryptedPayload(
        val metadata: ByteArray,
        val aad: ByteArray,
        val nonce: ByteArray,
        val ciphertext: ByteArray,
        val tag: ByteArray
    )

    fun metadata(
        domain: UniversalMessage.Domain,
        vin: String,
        epoch: ByteArray,
        expiresAt: Long,
        counter: Long,
        flags: Long
    ): ByteArray {

        require(vin.length == 17) {
            "VIN must contain exactly 17 characters"
        }

        require(epoch.size == 16) {
            "Tesla epoch must contain 16 bytes"
        }

        require(expiresAt in 0..0xffff_ffffL)
        require(counter in 0..0xffff_ffffL)
        require(flags in 0..0xffff_ffffL)

        val items =
            mutableListOf(
                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_SIGNATURE_TYPE
                            .number,
                    value =
                        byteArrayOf(
                            Signatures.SignatureType
                                .SIGNATURE_TYPE_AES_GCM_PERSONALIZED
                                .number
                                .toByte()
                        )
                ),

                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_DOMAIN
                            .number,
                    value =
                        byteArrayOf(
                            domain.number.toByte()
                        )
                ),

                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_PERSONALIZATION
                            .number,
                    value =
                        vin.toByteArray(
                            StandardCharsets.US_ASCII
                        )
                ),

                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_EPOCH
                            .number,
                    value =
                        epoch.copyOf()
                ),

                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_EXPIRES_AT
                            .number,
                    value =
                        TeslaMetadataSerializer
                            .uint32(expiresAt)
                ),

                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_COUNTER
                            .number,
                    value =
                        TeslaMetadataSerializer
                            .uint32(counter)
                )
            )

        if (flags != 0L) {

            items +=
                TeslaMetadataSerializer.Item(
                    tag =
                        Signatures.Tag
                            .TAG_FLAGS
                            .number,
                    value =
                        TeslaMetadataSerializer
                            .uint32(flags)
                )
        }

        return TeslaMetadataSerializer
            .serialize(items)
    }


    fun encrypt(
        plaintext: ByteArray,
        sharedKey: ByteArray,
        domain: UniversalMessage.Domain,
        vin: String,
        epoch: ByteArray,
        expiresAt: Long,
        counter: Long,
        flags: Long,
        nonce: ByteArray
    ): EncryptedPayload {

        require(sharedKey.size == 16) {
            "Tesla AES key must contain 16 bytes"
        }

        require(nonce.size == NONCE_SIZE) {
            "AES-GCM nonce must contain 12 bytes"
        }

        val metadata =
            metadata(
                domain = domain,
                vin = vin,
                epoch = epoch,
                expiresAt = expiresAt,
                counter = counter,
                flags = flags
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
            Cipher.ENCRYPT_MODE,
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

        val encrypted =
            cipher.doFinal(
                plaintext
            )

        require(
            encrypted.size >= TAG_SIZE
        )

        val ciphertext =
            encrypted.copyOfRange(
                0,
                encrypted.size - TAG_SIZE
            )

        val tag =
            encrypted.copyOfRange(
                encrypted.size - TAG_SIZE,
                encrypted.size
            )

        return EncryptedPayload(
            metadata = metadata,
            aad = aad,
            nonce = nonce.copyOf(),
            ciphertext = ciphertext,
            tag = tag
        )
    }
}
