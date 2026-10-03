package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.keys.Keys
import com.tesla.generated.vcsec.Vcsec


/**
 * Baut den von Tesla vorgesehenen BLE Add-Key-Request.
 *
 * Rolle:
 *   VEHICLE_MONITOR
 *
 * Form Factor:
 *   ANDROID_DEVICE
 *
 * Der Request selbst ist noch nicht der Nachweis einer
 * erfolgreichen Autorisierung. Die Bestätigung erfolgt
 * anschließend am Fahrzeug.
 */
object TeslaKeyEnrollmentRequest {

    fun build(
        publicKey: ByteArray
    ): ByteArray {

        require(
            publicKey.size == 65
        ) {
            "Tesla public key must be 65 bytes"
        }

        require(
            publicKey[0] == 0x04.toByte()
        ) {
            "Tesla public key must use uncompressed P-256 encoding"
        }


        val teslaPublicKey =
            Vcsec.PublicKey
                .newBuilder()
                .setPublicKeyRaw(
                    ByteString.copyFrom(
                        publicKey
                    )
                )
                .build()


        val permissionChange =
            Vcsec.PermissionChange
                .newBuilder()
                .setKey(
                    teslaPublicKey
                )
                .setKeyRole(
                    Keys.Role
                        .ROLE_VEHICLE_MONITOR
                )
                .build()


        val metadata =
            Vcsec.KeyMetadata
                .newBuilder()
                .setKeyFormFactor(
                    Vcsec.KeyFormFactor
                        .KEY_FORM_FACTOR_ANDROID_DEVICE
                )
                .build()


        val whitelistOperation =
            Vcsec.WhitelistOperation
                .newBuilder()
                .setAddKeyToWhitelistAndAddPermissions(
                    permissionChange
                )
                .setMetadataForKey(
                    metadata
                )
                .build()


        val unsignedMessage =
            Vcsec.UnsignedMessage
                .newBuilder()
                .setWhitelistOperation(
                    whitelistOperation
                )
                .build()


        val signedMessage =
            Vcsec.SignedMessage
                .newBuilder()
                .setProtobufMessageAsBytes(
                    ByteString.copyFrom(
                        unsignedMessage
                            .toByteArray()
                    )
                )
                .setSignatureType(
                    Vcsec.SignatureType
                        .SIGNATURE_TYPE_PRESENT_KEY
                )
                .build()


        return Vcsec.ToVCSECMessage
            .newBuilder()
            .setSignedMessage(
                signedMessage
            )
            .build()
            .toByteArray()
    }
}
