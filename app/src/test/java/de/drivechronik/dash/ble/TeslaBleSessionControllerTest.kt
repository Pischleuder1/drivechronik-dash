package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaBleSessionControllerTest {

    private val vin =
        "5YJ30123456789ABC"


    private val routingAddress =
        hex(
            "2c907bd76c640d360b3027dc7404efde"
        )


    private val epoch =
        hex(
            "4c463f9cc0d3d26906e982ed224adde6"
        )


    @Test
    fun completesAuthenticatedHandshake() {

        val client =
            ecKeyPair()

        val vehicle =
            ecKeyPair()


        var now =
            1_000_000_000L


        val controller =
            TeslaBleSessionController(
                vin =
                    vin,

                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,

                keyPair =
                    client,

                routingAddress =
                    routingAddress,

                nowNanos = {
                    now
                },

                randomBytes = { size ->

                    ByteArray(size) {
                        (it + 1).toByte()
                    }
                }
            )


        val request =
            controller
                .beginHandshake()


        assertEquals(
            TeslaProtocolSessionStatus
                .HANDSHAKE_PENDING,
            controller.status()
        )


        assertArrayEquals(
            routingAddress,
            request
                .message
                .fromDestination
                .routingAddress
                .toByteArray()
        )


        assertEquals(
            UniversalMessage
                .Domain
                .DOMAIN_INFOTAINMENT,
            request
                .message
                .toDestination
                .domain
        )


        val response =
            handshakeResponse(
                request =
                    request,

                client =
                    client,

                vehicle =
                    vehicle
            )


        now +=
            250_000_000L


        val state =
            controller
                .acceptHandshakeResponse(
                    response
                )


        assertEquals(
            TeslaProtocolSessionStatus.READY,
            controller.status()
        )


        assertEquals(
            6L,
            state.currentCounter()
        )


        assertArrayEquals(
            epoch,
            state.epoch()
        )
    }


    @Test
    fun createsEncryptedCommandAfterHandshake() {

        val client =
            ecKeyPair()

        val vehicle =
            ecKeyPair()


        var now =
            1_000_000_000L


        var randomCall =
            0


        val controller =
            TeslaBleSessionController(
                vin =
                    vin,

                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,

                keyPair =
                    client,

                routingAddress =
                    routingAddress,

                nowNanos = {
                    now
                },

                randomBytes = { size ->

                    randomCall++

                    ByteArray(size) { index ->
                        (
                            randomCall * 16 +
                                index
                            )
                            .toByte()
                    }
                }
            )


        val handshake =
            controller
                .beginHandshake()


        controller
            .acceptHandshakeResponse(
                handshakeResponse(
                    request =
                        handshake,

                    client =
                        client,

                    vehicle =
                        vehicle
                )
            )


        now +=
            1_000_000_000L


        val command =
            controller
                .buildEncryptedRequest(
                    TeslaDriveStateCodec
                        .buildRequest()
                )


        assertEquals(
            1,
            controller.pendingRequestCount()
        )


        assertNotNull(
            command.requestHash
        )


        assertEquals(
            17,
            command
                .requestHash!!
                .size
        )


        assertArrayEquals(
            routingAddress,
            command
                .message
                .fromDestination
                .routingAddress
                .toByteArray()
        )


        assertTrue(
            command
                .message
                .signatureData
                .hasAESGCMPersonalizedData()
        )


        assertEquals(
            7,
            command
                .message
                .signatureData
                .aesgcmPersonalizedData
                .counter
        )


        assertEquals(
            7L,
            controller
                .session()!!
                .currentCounter()
        )
    }


    @Test(
        expected =
            TeslaProtocolSessionException::class
    )
    fun rejectsWrongHandshakeAuthenticationTag() {

        val client =
            ecKeyPair()

        val vehicle =
            ecKeyPair()


        val controller =
            TeslaBleSessionController(
                vin =
                    vin,

                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,

                keyPair =
                    client,

                routingAddress =
                    routingAddress,

                nowNanos = {
                    1_000_000_000L
                },

                randomBytes = { size ->

                    ByteArray(size) {
                        (it + 1).toByte()
                    }
                }
            )


        val request =
            controller
                .beginHandshake()


        val valid =
            handshakeResponse(
                request =
                    request,

                client =
                    client,

                vehicle =
                    vehicle
            )


        val badTag =
            valid
                .signatureData
                .sessionInfoTag
                .tag
                .toByteArray()
                .also {

                    it[0] =
                        (
                            it[0].toInt()
                                xor 0x01
                            )
                            .toByte()
                }


        val badSignature =
            valid
                .signatureData
                .toBuilder()
                .setSessionInfoTag(
                    Signatures
                        .HMAC_Signature_Data
                        .newBuilder()
                        .setTag(
                            ByteString.copyFrom(
                                badTag
                            )
                        )
                )
                .build()


        val badResponse =
            valid
                .toBuilder()
                .setSignatureData(
                    badSignature
                )
                .build()


        controller
            .acceptHandshakeResponse(
                badResponse
            )
    }


    private fun handshakeResponse(
        request:
            TeslaOutboundProtocolMessage,

        client:
            KeyPair,

        vehicle:
            KeyPair
    ): UniversalMessage.RoutableMessage {

        val vehiclePublic =
            TeslaPublicKeyEncoding
                .encode(
                    vehicle.public
                        as ECPublicKey
                )


        val info =
            Signatures
                .SessionInfo
                .newBuilder()
                .setCounter(
                    6
                )
                .setPublicKey(
                    ByteString.copyFrom(
                        vehiclePublic
                    )
                )
                .setEpoch(
                    ByteString.copyFrom(
                        epoch
                    )
                )
                .setClockTime(
                    2650
                )
                .build()


        val infoBytes =
            info.toByteArray()


        val sharedKey =
            TeslaSessionCrypto
                .deriveSharedKey(
                    privateKey =
                        client.private,

                    vehiclePublicKey =
                        vehicle.public
                            as ECPublicKey
                )


        val tag =
            TeslaSessionInfoAuthenticator
                .expectedTag(
                    sharedKey =
                        sharedKey,

                    vin =
                        vin,

                    challenge =
                        request.uuid,

                    sessionInfoBytes =
                        infoBytes
                )


        val signature =
            Signatures
                .SignatureData
                .newBuilder()
                .setSessionInfoTag(
                    Signatures
                        .HMAC_Signature_Data
                        .newBuilder()
                        .setTag(
                            ByteString.copyFrom(
                                tag
                            )
                        )
                )
                .build()


        return UniversalMessage
            .RoutableMessage
            .newBuilder()
            .setToDestination(
                UniversalMessage
                    .Destination
                    .newBuilder()
                    .setRoutingAddress(
                        ByteString.copyFrom(
                            routingAddress
                        )
                    )
            )
            .setFromDestination(
                UniversalMessage
                    .Destination
                    .newBuilder()
                    .setDomain(
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT
                    )
            )
            .setSessionInfo(
                ByteString.copyFrom(
                    infoBytes
                )
            )
            .setSignatureData(
                signature
            )
            .setRequestUuid(
                ByteString.copyFrom(
                    request.uuid
                )
            )
            .build()
    }


    private fun ecKeyPair():
        KeyPair {

        val generator =
            KeyPairGenerator
                .getInstance(
                    "EC"
                )


        generator.initialize(
            ECGenParameterSpec(
                "secp256r1"
            )
        )


        return generator
            .generateKeyPair()
    }


    private fun hex(
        value: String
    ): ByteArray {

        return ByteArray(
            value.length / 2
        ) { index ->

            value
                .substring(
                    index * 2,
                    index * 2 + 2
                )
                .toInt(16)
                .toByte()
        }
    }
}
