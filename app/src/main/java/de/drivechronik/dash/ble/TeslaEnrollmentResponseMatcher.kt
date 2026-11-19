package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage

object TeslaEnrollmentResponseMatcher {

    fun matches(
        request: UniversalMessage.RoutableMessage,
        response: UniversalMessage.RoutableMessage
    ): Boolean {
        val requestUuid =
            request.uuid.toByteArray()

        val responseUuid =
            response.requestUuid.toByteArray()

        return requestUuid.size == 16 &&
            responseUuid.contentEquals(requestUuid) &&
            response.fromDestination.domain ==
                UniversalMessage.Domain.DOMAIN_VEHICLE_SECURITY &&
            response.hasSignedMessageStatus()
    }
}
