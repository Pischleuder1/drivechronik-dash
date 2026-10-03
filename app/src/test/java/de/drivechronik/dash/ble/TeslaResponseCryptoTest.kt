package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import java.security.MessageDigest
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaResponseCryptoTest {

    private val sharedKey =
        hex(
            "1b2fce19967b79db696f909cff89ea9a"
        )

    private val requestHash =
        hex(
            "05" +
                "c228e0ff64991481db3a7bbc133696c5"
        )


    @Test
    fun serializesResponseMetadata() {

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

        assertArrayEquals(
            hex(
                "000109" +
                "010103" +
                "021135594a3330313233343536373839414243" +
                "05040000000a" +
                "070400000000" +
                "0811" +
                "05c228e0ff64991481db3a7bbc133696c5" +
                "090400000000" +
                "ff"
            ),
            metadata
        )
    }


    @Test
    fun decryptsAuthenticatedResponse() {

        val plaintext =
            hex(
                "0a020801"
            )

        val nonce =
            hex(
                "00112233445566778899aabb"
            )

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
                plaintext = plaintext,
                key = sharedKey,
                nonce = nonce,
                metadata = metadata
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

        val result =
            TeslaResponseCrypto
                .decrypt(
                    ciphertext = ciphertext,
                    sharedKey = sharedKey,
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
                    fault = 0,
                    nonce = nonce,
                    tag = tag
                )

        assertArrayEquals(
            plaintext,
            result.plaintext
        )
    }


    @Test
    fun rejectsWrongRequestHash() {

        val plaintext =
            hex(
                "0a020801"
            )

        val nonce =
            hex(
                "00112233445566778899aabb"
            )

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
                plaintext = plaintext,
                key = sharedKey,
                nonce = nonce,
                metadata = metadata
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

        val wrongHash =
            requestHash.copyOf().also {
                it[it.lastIndex] =
                    (
                        it.last().toInt()
                            xor 0x01
                    ).toByte()
            }

        var rejected = false

        try {

            TeslaResponseCrypto
                .decrypt(
                    ciphertext = ciphertext,
                    sharedKey = sharedKey,
                    domain =
                        UniversalMessage
                            .Domain
                            .DOMAIN_INFOTAINMENT,
                    vin =
                        "5YJ30123456789ABC",
                    counter = 10,
                    flags = 0,
                    requestHash =
                        wrongHash,
                    fault = 0,
                    nonce = nonce,
                    tag = tag
                )
        }
        catch (
            e: AEADBadTagException
        ) {
            rejected = true
        }

        assertTrue(
            rejected
        )
    }


    @Test
    fun replayGuardAllowsOutOfOrderButRejectsDuplicate() {

        val guard =
            TeslaResponseReplayGuard()

        assertTrue(
            guard.accept(
                requestHash,
                12
            )
        )

        /*
         * Out-of-order ist erlaubt.
         */
        assertTrue(
            guard.accept(
                requestHash,
                10
            )
        )

        assertTrue(
            guard.accept(
                requestHash,
                11
            )
        )

        /*
         * Derselbe Counter für denselben
         * Request ist Replay.
         */
        assertFalse(
            guard.accept(
                requestHash,
                10
            )
        )

        /*
         * Derselbe Counter bei einem anderen
         * Request ist erlaubt.
         */
        val otherRequest =
            requestHash.copyOf().also {
                it[it.lastIndex] =
                    (
                        it.last().toInt()
                            xor 0x01
                    ).toByte()
            }

        assertTrue(
            guard.accept(
                otherRequest,
                10
            )
        )
    }


    private fun encryptForTest(
        plaintext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
        metadata: ByteArray
    ): ByteArray {

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
                key,
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
                .toInt(16)
                .toByte()
        }
    }
}
