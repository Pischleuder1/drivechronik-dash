package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.signatures.Signatures
import com.tesla.generated.universalmessage.UniversalMessage
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaRoutableResponseProcessorTest {

    private val sharedKey =
        hex(
            "1b2fce19967b79db696f909cff89ea9a"
        )

    private val requestHash =
        hex(
            "05" +
                "c228e0ff64991481db3a7bbc133696c5"
        )

    private val requestUuid =
        hex(
            "58406580528b6a5301391800b4fe9b99"
        )

    private val nonce =
        hex(
            "00112233445566778899aabb"
        )

    private val plaintext =
        hex(
            "0a020801"
        )


    @Test
    fun decryptsEncryptedRoutableResponse() {

        val message =
            encryptedResponse()

        val result =
            TeslaRoutableResponseProcessor
                .process(
                    message = message,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    sharedKey =
                        sharedKey,
                    requestHash =
                        requestHash,
                    replayGuard =
                        TeslaResponseReplayGuard(),
                    expectedRequestUuid =
                        requestUuid
                )

        assertTrue(
            result.encrypted
        )

        assertEquals(
            10L,
            result.responseCounter
        )

        assertArrayEquals(
            plaintext,
            result.payload
        )

        assertEquals(
            UniversalMessage
                .MessageFault_E
                .MESSAGEFAULT_ERROR_NONE,
            result.fault
        )
    }


    @Test(
        expected =
            TeslaResponseReplayException::class
    )
    fun rejectsDuplicateAuthenticatedResponse() {

        val guard =
            TeslaResponseReplayGuard()

        val message =
            encryptedResponse()

        TeslaRoutableResponseProcessor
            .process(
                message = message,
                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,
                vin =
                    "5YJ30123456789ABC",
                sharedKey =
                    sharedKey,
                requestHash =
                    requestHash,
                replayGuard =
                    guard,
                expectedRequestUuid =
                    requestUuid
            )

        /*
         * Exakt dieselbe authentifizierte
         * Antwort darf nicht noch einmal
         * akzeptiert werden.
         */
        TeslaRoutableResponseProcessor
            .process(
                message = message,
                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,
                vin =
                    "5YJ30123456789ABC",
                sharedKey =
                    sharedKey,
                requestHash =
                    requestHash,
                replayGuard =
                    guard,
                expectedRequestUuid =
                    requestUuid
            )
    }


    @Test
    fun invalidAuthenticationDoesNotConsumeCounter() {

        val guard =
            TeslaResponseReplayGuard()

        val good =
            encryptedResponse()

        val badAes =
            good
                .signatureData
                .aesgcmResponseData
                .toBuilder()
                .setTag(
                    ByteString.copyFrom(
                        good
                            .signatureData
                            .aesgcmResponseData
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
                    )
                )
                .build()

        val badSignature =
            good
                .signatureData
                .toBuilder()
                .setAESGCMResponseData(
                    badAes
                )
                .build()

        val bad =
            good
                .toBuilder()
                .setSignatureData(
                    badSignature
                )
                .build()


        var authenticationFailed =
            false

        try {

            TeslaRoutableResponseProcessor
                .process(
                    message = bad,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    sharedKey =
                        sharedKey,
                    requestHash =
                        requestHash,
                    replayGuard =
                        guard,
                    expectedRequestUuid =
                        requestUuid
                )
        }
        catch (
            e: AEADBadTagException
        ) {

            authenticationFailed =
                true
        }


        assertTrue(
            authenticationFailed
        )


        /*
         * Derselbe Counter muss danach
         * mit einer gültigen Antwort noch
         * akzeptiert werden.
         */
        val result =
            TeslaRoutableResponseProcessor
                .process(
                    message = good,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    sharedKey =
                        sharedKey,
                    requestHash =
                        requestHash,
                    replayGuard =
                        guard,
                    expectedRequestUuid =
                        requestUuid
                )

        assertArrayEquals(
            plaintext,
            result.payload
        )
    }


    @Test
    fun acceptsPlaintextFallback() {

        val message =
            UniversalMessage
                .RoutableMessage
                .newBuilder()
                .setProtobufMessageAsBytes(
                    ByteString.copyFrom(
                        plaintext
                    )
                )
                .setRequestUuid(
                    ByteString.copyFrom(
                        requestUuid
                    )
                )
                .setSignedMessageStatus(
                    normalStatus()
                )
                .build()


        val result =
            TeslaRoutableResponseProcessor
                .process(
                    message = message,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    sharedKey =
                        sharedKey,
                    requestHash =
                        requestHash,
                    replayGuard =
                        TeslaResponseReplayGuard(),
                    expectedRequestUuid =
                        requestUuid
                )


        assertFalse(
            result.encrypted
        )

        assertEquals(
            null,
            result.responseCounter
        )

        assertArrayEquals(
            plaintext,
            result.payload
        )
    }


    @Test(
        expected =
            TeslaResponseUuidMismatchException::class
    )
    fun rejectsWrongRequestUuidWhenPresent() {

        val wrongUuid =
            requestUuid.copyOf().also {
                it[0] =
                    (
                        it[0].toInt()
                            xor 0x01
                    )
                        .toByte()
            }

        val message =
            encryptedResponse()
                .toBuilder()
                .setRequestUuid(
                    ByteString.copyFrom(
                        wrongUuid
                    )
                )
                .build()


        TeslaRoutableResponseProcessor
            .process(
                message = message,
                domain =
                    UniversalMessage
                        .Domain
                        .DOMAIN_INFOTAINMENT,
                vin =
                    "5YJ30123456789ABC",
                sharedKey =
                    sharedKey,
                requestHash =
                    requestHash,
                replayGuard =
                    TeslaResponseReplayGuard(),
                expectedRequestUuid =
                    requestUuid
            )
    }


    private fun encryptedResponse():
        UniversalMessage.RoutableMessage {

        val metadata =
            TeslaResponseCrypto
                .metadata(
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    counter = 10,
                    flags = 0,
                    requestHash =
                        requestHash,
                    fault = 0
                )


        val encrypted =
            encryptForTest(
                plaintext =
                    plaintext,
                metadata =
                    metadata
            )


        val ciphertext =
            encrypted.copyOfRange(
                0,
                encrypted.size - 16
            )


        val tag =
            encrypted.copyOfRange(
                encrypted.size - 16,
                encrypted.size
            )


        val responseData =
            Signatures
                .AES_GCM_Response_Signature_Data
                .newBuilder()
                .setNonce(
                    ByteString.copyFrom(
                        nonce
                    )
                )
                .setCounter(
                    10
                )
                .setTag(
                    ByteString.copyFrom(
                        tag
                    )
                )
                .build()


        val signature =
            Signatures
                .SignatureData
                .newBuilder()
                .setAESGCMResponseData(
                    responseData
                )
                .build()


        return UniversalMessage
            .RoutableMessage
            .newBuilder()
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
            .setProtobufMessageAsBytes(
                ByteString.copyFrom(
                    ciphertext
                )
            )
            .setSignatureData(
                signature
            )
            .setSignedMessageStatus(
                normalStatus()
            )
            .setRequestUuid(
                ByteString.copyFrom(
                    requestUuid
                )
            )
            .setFlags(
                0
            )
            .build()
    }


    private fun normalStatus():
        UniversalMessage.MessageStatus {

        return UniversalMessage
            .MessageStatus
            .newBuilder()
            .setOperationStatus(
                UniversalMessage
                    .OperationStatus_E
                    .OPERATIONSTATUS_OK
            )
            .setSignedMessageFault(
                UniversalMessage
                    .MessageFault_E
                    .MESSAGEFAULT_ERROR_NONE
            )
            .build()
    }


    private fun encryptForTest(
        plaintext: ByteArray,
        metadata: ByteArray
    ): ByteArray {

        val aad =
            MessageDigest
                .getInstance(
                    "SHA-256"
                )
                .digest(
                    metadata
                )


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
                128,
                nonce
            )
        )


        cipher.updateAAD(
            aad
        )


        return cipher.doFinal(
            plaintext
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
                .toInt(
                    16
                )
                .toByte()
        }
    }
}
