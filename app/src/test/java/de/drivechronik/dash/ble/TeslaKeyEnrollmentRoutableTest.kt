package de.drivechronik.dash.ble

import com.tesla.generated.keys.Keys
import com.tesla.generated.vcsec.Vcsec
import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.*
import org.junit.Test

class TeslaKeyEnrollmentRoutableTest {

    @Test
    fun buildsRoutableVehicleMonitorEnrollment() {
        val publicKey = ByteArray(65) {
            it.toByte()
        }
        publicKey[0] = 0x04

        val bytes =
            TeslaKeyEnrollmentRequest.buildRoutable(
                publicKey
            )

        val message =
            UniversalMessage.RoutableMessage
                .parseFrom(bytes)

        assertEquals(
            UniversalMessage.Domain.DOMAIN_VEHICLE_SECURITY,
            message.toDestination.domain
        )

        assertEquals(
            16,
            message.fromDestination.routingAddress.size()
        )

        assertEquals(16, message.uuid.size())

        val unsigned =
            Vcsec.UnsignedMessage.parseFrom(
                message.protobufMessageAsBytes
            )

        assertTrue(unsigned.hasWhitelistOperation())

        val operation = unsigned.whitelistOperation
        val permission =
            operation.addKeyToWhitelistAndAddPermissions

        assertEquals(
            Keys.Role.ROLE_VEHICLE_MONITOR,
            permission.keyRole
        )

        assertEquals(
            Vcsec.KeyFormFactor.KEY_FORM_FACTOR_ANDROID_DEVICE,
            operation.metadataForKey.keyFormFactor
        )

        assertArrayEquals(
            publicKey,
            permission.key.publicKeyRaw.toByteArray()
        )
    }
}
