package de.drivechronik.dash.ble

import com.tesla.generated.universalmessage.UniversalMessage
import org.junit.Assert.assertEquals
import org.junit.Test

class TeslaBlePipelineTest {

    @Test
    fun completeBlePipelinePreservesRoutableMessage() {

        val original =
            UniversalMessage
                .RoutableMessage
                .newBuilder()
                .setToDestination(
                    UniversalMessage
                        .Destination
                        .newBuilder()
                        .setDomain(
                            UniversalMessage
                                .Domain
                                .DOMAIN_INFOTAINMENT
                        )
                )
                .setFlags(1)
                .build()

        val protobuf =
            TeslaRoutableMessageCodec
                .encode(original)

        val frame =
            TeslaBleFrameCodec
                .encode(protobuf)

        /*
         * Absichtlich sehr kleine Stücke,
         * damit Fragmentierung sicher
         * getestet wird.
         */
        val chunks =
            TeslaBleFrameCodec.chunk(
                frame,
                7
            )

        val decoder =
            TeslaBleFrameDecoder()

        val messages =
            mutableListOf<ByteArray>()

        chunks.forEach {
            messages +=
                decoder.feed(it)
        }

        assertEquals(
            1,
            messages.size
        )

        val decoded =
            TeslaRoutableMessageCodec
                .decode(messages.single())

        assertEquals(
            original,
            decoded
        )

        assertEquals(
            0,
            decoder.bufferedByteCount()
        )
    }
}
