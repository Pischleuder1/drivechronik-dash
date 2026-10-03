package de.drivechronik.dash.ble

import com.tesla.generated.keys.Keys
import com.tesla.generated.vcsec.Vcsec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaKeyEnrollmentRequestTest {

    @Test
    fun buildsVehicleMonitorAndroidEnrollmentRequest() {

        val publicKey =
            ByteArray(65) {
                it.toByte()
            }.also {
                it[0] =
                    0x04
            }


        val encoded =
            TeslaKeyEnrollmentRequest
                .build(
                    publicKey
                )


        assertTrue(
            encoded.isNotEmpty()
        )


        val envelope =
            Vcsec.ToVCSECMessage
                .parseFrom(
                    encoded
                )


        assertEquals(
            Vcsec.SignatureType
                .SIGNATURE_TYPE_PRESENT_KEY,

            envelope
                .signedMessage
                .signatureType
        )


        val unsigned =
            Vcsec.UnsignedMessage
                .parseFrom(
                    envelope
                        .signedMessage
                        .protobufMessageAsBytes
                )


        val operation =
            unsigned
                .whitelistOperation


        val permission =
            operation
                .addKeyToWhitelistAndAddPermissions


        assertEquals(
            Keys.Role
                .ROLE_VEHICLE_MONITOR,

            permission
                .keyRole
        )


        assertEquals(
            Vcsec.KeyFormFactor
                .KEY_FORM_FACTOR_ANDROID_DEVICE,

            operation
                .metadataForKey
                .keyFormFactor
        )


        assertArrayEquals(
            publicKey,

            permission
                .key
                .publicKeyRaw
                .toByteArray()
        )
    }


    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun rejectsInvalidPublicKey() {

        TeslaKeyEnrollmentRequest
            .build(
                ByteArray(64)
            )
    }
}
