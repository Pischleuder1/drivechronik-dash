package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.universalmessage.UniversalMessage
import java.security.SecureRandom

object TeslaSessionInfoRequestBuilder {

    const val ROUTING_ADDRESS_SIZE = 16
    const val UUID_SIZE = 16

    private val secureRandom =
        SecureRandom()

    fun build(
        publicKey: ByteArray,
        domain: UniversalMessage.Domain,
        routingAddress: ByteArray,
        uuid: ByteArray
    ): UniversalMessage.RoutableMessage {

        require(
            publicKey.size ==
                TeslaPublicKeyEncoding.ENCODED_SIZE
        ) {
            "Tesla public key must be 65 bytes"
        }

        require(
            routingAddress.size ==
                ROUTING_ADDRESS_SIZE
        ) {
            "Routing address must be 16 bytes"
        }

        require(
            uuid.size in 1..UUID_SIZE
        ) {
            "UUID must contain 1..16 bytes"
        }

        val request =
            UniversalMessage
                .SessionInfoRequest
                .newBuilder()
                .setPublicKey(
                    ByteString.copyFrom(
                        publicKey
                    )
                )
                .build()

        val toDestination =
            UniversalMessage
                .Destination
                .newBuilder()
                .setDomain(domain)
                .build()

        val fromDestination =
            UniversalMessage
                .Destination
                .newBuilder()
                .setRoutingAddress(
                    ByteString.copyFrom(
                        routingAddress
                    )
                )
                .build()

        return UniversalMessage
            .RoutableMessage
            .newBuilder()
            .setToDestination(
                toDestination
            )
            .setFromDestination(
                fromDestination
            )
            .setSessionInfoRequest(
                request
            )
            .setUuid(
                ByteString.copyFrom(
                    uuid
                )
            )
            .setFlags(
                1 shl
                    UniversalMessage
                        .Flags
                        .FLAG_ENCRYPT_RESPONSE
                        .number
            )
            .build()
    }


    fun buildRandom(
        publicKey: ByteArray,
        domain: UniversalMessage.Domain
    ): UniversalMessage.RoutableMessage {

        return build(
            publicKey = publicKey,
            domain = domain,
            routingAddress =
                randomBytes(
                    ROUTING_ADDRESS_SIZE
                ),
            uuid =
                randomBytes(
                    UUID_SIZE
                )
        )
    }


    private fun randomBytes(
        size: Int
    ): ByteArray {

        return ByteArray(size).also {
            secureRandom.nextBytes(it)
        }
    }
}
