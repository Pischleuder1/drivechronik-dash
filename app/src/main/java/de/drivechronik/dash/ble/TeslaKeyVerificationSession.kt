package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import java.security.KeyPair

/**
 * Ausschließlich zur Prüfung eines vorhandenen Tesla-Schlüssels.
 *
 * Keine Fahrzeugbefehle.
 * Keine Änderung der Whitelist.
 * Keine automatische Live-Freigabe.
 */
class TeslaKeyVerificationSession(
    vin: String,
    keyPair: KeyPair
) {
    private val controller =
        TeslaBleSessionController(
            vin = vin,
            domain =
                UniversalMessage.Domain.DOMAIN_INFOTAINMENT,
            keyPair = keyPair
        )

    fun begin(): UniversalMessage.RoutableMessage {
        return controller.beginHandshake().message
    }

    fun verify(
        response: UniversalMessage.RoutableMessage
    ): Boolean {
        controller.acceptHandshakeResponse(response)

        val verified =
            controller.status() ==
                TeslaProtocolSessionStatus.READY

        // Die Prüfsitzung wird nicht für Befehle genutzt.
        controller.clear()

        return verified
    }

    fun clear() {
        controller.clear()
    }
}
