package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage

object TeslaEncryptedCommandBuilder {

    data class Result(
        val message: UniversalMessage.RoutableMessage,
        val requestHash: ByteArray,
        val ciphertext: ByteArray,
        val authenticationTag: ByteArray
    )


    fun build(
        plaintext: ByteArray,
        clientPublicKey: ByteArray,
        sharedKey: ByteArray,
        domain: UniversalMessage.Domain,
        vin: String,
        epoch: ByteArray,
        routingAddress: ByteArray,
        uuid: ByteArray,
        expiresAt: Long,
        counter: Long,
        flags: Long,
        nonce: ByteArray
    ): Result {

        require(
            clientPublicKey.size ==
                TeslaPublicKeyEncoding.ENCODED_SIZE
        ) {
            "Tesla client public key must contain 65 bytes"
        }

        require(
            routingAddress.size == 16
        ) {
            "Routing address must contain 16 bytes"
        }

        require(
            uuid.isNotEmpty() &&
                uuid.size <= 16
        ) {
            "UUID must contain 1..16 bytes"
        }

        require(
            counter in 0..0xffff_ffffL
        )

        require(
            expiresAt in 0..0xffff_ffffL
        )

        require(
            flags in 0..0xffff_ffffL
        )


        val encrypted =
            TeslaCommandCrypto.encrypt(
                plaintext = plaintext,
                sharedKey = sharedKey,
                domain = domain,
                vin = vin,
                epoch = epoch,
                expiresAt = expiresAt,
                counter = counter,
                flags = flags,
                nonce = nonce
            )


        val signerIdentity =
            Signatures
                .KeyIdentity
                .newBuilder()
                .setPublicKey(
                    ByteString.copyFrom(
                        clientPublicKey
                    )
                )
                .build()


        val aesSignature =
            Signatures
                .AES_GCM_Personalized_Signature_Data
                .newBuilder()
                .setEpoch(
                    ByteString.copyFrom(
                        epoch
                    )
                )
                .setNonce(
                    ByteString.copyFrom(
                        nonce
                    )
                )
                .setCounter(
                    counter.toInt()
                )
                .setExpiresAt(
                    expiresAt.toInt()
                )
                .setTag(
                    ByteString.copyFrom(
                        encrypted.tag
                    )
                )
                .build()


        val signatureData =
            Signatures
                .SignatureData
                .newBuilder()
                .setSignerIdentity(
                    signerIdentity
                )
                .setAESGCMPersonalizedData(
                    aesSignature
                )
                .build()


        val message =
            UniversalMessage
                .RoutableMessage
                .newBuilder()
                .setToDestination(
                    UniversalMessage
                        .Destination
                        .newBuilder()
                        .setDomain(
                            domain
                        )
                )
                .setFromDestination(
                    UniversalMessage
                        .Destination
                        .newBuilder()
                        .setRoutingAddress(
                            ByteString.copyFrom(
                                routingAddress
                            )
                        )
                )
                .setProtobufMessageAsBytes(
                    ByteString.copyFrom(
                        encrypted.ciphertext
                    )
                )
                .setSignatureData(
                    signatureData
                )
                .setFlags(
                    flags.toInt()
                )
                .setUuid(
                    ByteString.copyFrom(
                        uuid
                    )
                )
                .build()


        val requestHash =
            byteArrayOf(
                Signatures
                    .SignatureType
                    .SIGNATURE_TYPE_AES_GCM_PERSONALIZED
                    .number
                    .toByte()
            ) +
                encrypted.tag


        require(
            requestHash.size == 17
        ) {
            "AES-GCM request hash must contain 17 bytes"
        }


        return Result(
            message = message,
            requestHash = requestHash,
            ciphertext =
                encrypted.ciphertext.copyOf(),
            authenticationTag =
                encrypted.tag.copyOf()
        )
    }
}
