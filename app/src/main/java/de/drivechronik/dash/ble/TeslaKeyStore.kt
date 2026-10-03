package de.drivechronik.dash.ble

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec

object TeslaKeyStore {

    private const val ANDROID_KEYSTORE =
        "AndroidKeyStore"

    private const val KEY_ALIAS =
        "drivechronik_tesla_p256"

    fun getOrCreateKeyPair(): KeyPair {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        val existing =
            keyStore.getEntry(
                KEY_ALIAS,
                null
            )

        if (
            existing is
                KeyStore.PrivateKeyEntry
        ) {

            return KeyPair(
                existing.certificate.publicKey,
                existing.privateKey
            )
        }

        val generator =
            KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_EC,
                ANDROID_KEYSTORE
            )

        val spec =
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_AGREE_KEY
            )
                .setAlgorithmParameterSpec(
                    ECGenParameterSpec(
                        "secp256r1"
                    )
                )
                .build()

        generator.initialize(spec)

        return generator.generateKeyPair()
    }

    fun encodedPublicKey(): ByteArray {

        val publicKey =
            getOrCreateKeyPair()
                .public as ECPublicKey

        return TeslaPublicKeyEncoding
            .encode(publicKey)
    }

    fun hasKey(): Boolean {

        val keyStore =
            KeyStore.getInstance(
                ANDROID_KEYSTORE
            ).apply {
                load(null)
            }

        return keyStore.containsAlias(
            KEY_ALIAS
        )
    }
}
