package de.drivechronik.dash.ble

import com.google.protobuf.ByteString
import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.*
import org.junit.Test

class TeslaEnrollmentResponseMatcherTest {

    private val uuid = ByteArray(16) {
        it.toByte()
    }

    private fun request() =
        UniversalMessage.RoutableMessage.newBuilder()
            .setUuid(ByteString.copyFrom(uuid))
            .build()

    private fun response(
        responseUuid: ByteArray,
        domain: UniversalMessage.Domain =
            UniversalMessage.Domain.DOMAIN_VEHICLE_SECURITY,
        withStatus: Boolean = true
    ): UniversalMessage.RoutableMessage {
        val builder =
            UniversalMessage.RoutableMessage.newBuilder()
                .setFromDestination(
                    UniversalMessage.Destination.newBuilder()
                        .setDomain(domain)
                )
                .setRequestUuid(
                    ByteString.copyFrom(responseUuid)
                )

        if (withStatus) {
            builder.setSignedMessageStatus(
                UniversalMessage.MessageStatus.newBuilder()
                    .setOperationStatus(
                        UniversalMessage.OperationStatus_E
                            .OPERATIONSTATUS_WAIT
                    )
            )
        }

        return builder.build()
    }

    @Test
    fun acceptsMatchingResponse() {
        assertTrue(
            TeslaEnrollmentResponseMatcher.matches(
                request(),
                response(uuid)
            )
        )
    }

    @Test
    fun rejectsWrongUuid() {
        assertFalse(
            TeslaEnrollmentResponseMatcher.matches(
                request(),
                response(ByteArray(16) { 99.toByte() })
            )
        )
    }

    @Test
    fun rejectsMissingRequestUuid() {
        assertFalse(
            TeslaEnrollmentResponseMatcher.matches(
                request(),
                response(byteArrayOf())
            )
        )
    }

    @Test
    fun rejectsWrongDomain() {
        assertFalse(
            TeslaEnrollmentResponseMatcher.matches(
                request(),
                response(
                    uuid,
                    UniversalMessage.Domain.DOMAIN_INFOTAINMENT
                )
            )
        )
    }

    @Test
    fun rejectsMissingStatus() {
        assertFalse(
            TeslaEnrollmentResponseMatcher.matches(
                request(),
                response(uuid, withStatus = false)
            )
        )
    }
}
