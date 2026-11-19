package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.*
import org.junit.Test

class TeslaEnrollmentRealResponseTest {

    private fun hex(value: String): ByteArray =
        value.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()

    @Test
    fun matchesCapturedModelYWaitResponse() {
        val captured = hex(
            "3212121020f48b5a6afab7b5d99c81cc0470f9db" +
            "3a020802620208019203108238ece03d8927d507d6927b89071e7d"
        )

        val response =
            UniversalMessage.RoutableMessage
                .parseFrom(captured)

        assertEquals(
            UniversalMessage.OperationStatus_E.OPERATIONSTATUS_WAIT,
            response.signedMessageStatus.operationStatus
        )

        assertEquals(
            UniversalMessage.MessageFault_E.MESSAGEFAULT_ERROR_NONE,
            response.signedMessageStatus.signedMessageFault
        )

        assertEquals(16, response.requestUuid.size())

        val matchingRequest =
            UniversalMessage.RoutableMessage.newBuilder()
                .setUuid(response.requestUuid)
                .build()

        assertTrue(
            TeslaEnrollmentResponseMatcher.matches(
                matchingRequest, response
            )
        )

        val wrongRequest =
            UniversalMessage.RoutableMessage.newBuilder()
                .setUuid(ByteString.copyFrom(ByteArray(16)))
                .build()

        assertFalse(
            TeslaEnrollmentResponseMatcher.matches(
                wrongRequest, response
            )
        )
    }
}
