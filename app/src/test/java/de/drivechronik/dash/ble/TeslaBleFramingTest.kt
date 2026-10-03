package de.drivechronik.dash.ble

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaBleFramingTest {

    @Test
    fun encodeUsesTwoByteBigEndianLength() {

        val payload =
            ByteArray(300) {
                (it and 0xff).toByte()
            }

        val frame =
            TeslaBleFrameCodec.encode(
                payload
            )

        assertEquals(
            302,
            frame.size
        )

        assertEquals(
            0x01,
            frame[0].toInt() and 0xff
        )

        assertEquals(
            0x2c,
            frame[1].toInt() and 0xff
        )

        assertArrayEquals(
            payload,
            frame.copyOfRange(
                2,
                frame.size
            )
        )
    }


    @Test
    fun decoderHandlesSplitHeaderAndPayload() {

        val payload =
            byteArrayOf(
                0x32,
                0x02,
                0x08,
                0x03,
                0x52,
                0x04
            )

        val frame =
            TeslaBleFrameCodec.encode(
                payload
            )

        val decoder =
            TeslaBleFrameDecoder()

        assertTrue(
            decoder.feed(
                frame.copyOfRange(0, 1)
            ).isEmpty()
        )

        assertTrue(
            decoder.feed(
                frame.copyOfRange(1, 4)
            ).isEmpty()
        )

        val messages =
            decoder.feed(
                frame.copyOfRange(
                    4,
                    frame.size
                )
            )

        assertEquals(
            1,
            messages.size
        )

        assertArrayEquals(
            payload,
            messages[0]
        )

        assertEquals(
            0,
            decoder.bufferedByteCount()
        )
    }


    @Test
    fun decoderHandlesMultipleFramesAtOnce() {

        val first =
            byteArrayOf(
                1,
                2,
                3
            )

        val second =
            byteArrayOf(
                4,
                5,
                6,
                7,
                8
            )

        val combined =
            TeslaBleFrameCodec.encode(first) +
                TeslaBleFrameCodec.encode(second)

        val decoder =
            TeslaBleFrameDecoder()

        val messages =
            decoder.feed(combined)

        assertEquals(
            2,
            messages.size
        )

        assertArrayEquals(
            first,
            messages[0]
        )

        assertArrayEquals(
            second,
            messages[1]
        )
    }


    @Test
    fun chunkReassemblesOriginalFrame() {

        val payload =
            ByteArray(400) {
                (it and 0xff).toByte()
            }

        val frame =
            TeslaBleFrameCodec.encode(
                payload
            )

        val chunks =
            TeslaBleFrameCodec.chunk(
                frame,
                112
            )

        assertTrue(
            chunks.size > 1
        )

        val rebuilt =
            chunks.fold(
                ByteArray(0)
            ) { acc, bytes ->
                acc + bytes
            }

        assertArrayEquals(
            frame,
            rebuilt
        )
    }


    @Test(
        expected =
            IllegalArgumentException::class
    )
    fun encoderRejectsOversizedMessage() {

        TeslaBleFrameCodec.encode(
            ByteArray(
                TeslaBleFrameCodec
                    .MAX_MESSAGE_SIZE + 1
            )
        )
    }
}
