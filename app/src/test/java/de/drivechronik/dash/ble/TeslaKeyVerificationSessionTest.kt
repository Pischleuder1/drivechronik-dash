package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import org.junit.Assert.*
import org.junit.Test

class TeslaKeyVerificationSessionTest {

    private fun newKeyPair() =
        KeyPairGenerator.getInstance("EC").apply {
            initialize(ECGenParameterSpec("secp256r1"))
        }.generateKeyPair()

    @Test
    fun beginsReadOnlyVerificationHandshake() {
        val verifier =
            TeslaKeyVerificationSession(
                vin = "5YJ3E1EA7KF000001",
                keyPair = newKeyPair()
            )

        val request = verifier.begin()

        assertEquals(
            UniversalMessage.Domain.DOMAIN_INFOTAINMENT,
            request.toDestination.domain
        )

        assertTrue(request.hasSessionInfoRequest())
        assertEquals(16, request.uuid.size())

        verifier.clear()
    }

    @Test
    fun rejectsUnverifiedResponse() {
        val verifier =
            TeslaKeyVerificationSession(
                vin = "5YJ3E1EA7KF000001",
                keyPair = newKeyPair()
            )

        verifier.begin()

        val invalidResponse =
            UniversalMessage.RoutableMessage.newBuilder()
                .setFromDestination(
                    UniversalMessage.Destination.newBuilder()
                        .setDomain(
                            UniversalMessage.Domain.DOMAIN_INFOTAINMENT
                        )
                )
                .build()

        var rejected = false

        try {
            verifier.verify(invalidResponse)
        } catch (e: TeslaProtocolSessionException) {
            rejected = true
        } finally {
            verifier.clear()
        }

        assertTrue(rejected)
    }
}
