package de.drivechronik.dash.ble

object TeslaBleFrameCodec {

    const val HEADER_SIZE = 2
    const val MAX_MESSAGE_SIZE = 0xffff

    fun encode(
        message: ByteArray
    ): ByteArray {

        require(
            message.size <= MAX_MESSAGE_SIZE
        ) {
            "Tesla BLE message too large: ${message.size}"
        }

        val frame =
            ByteArray(
                HEADER_SIZE + message.size
            )

        frame[0] =
            ((message.size ushr 8) and 0xff)
                .toByte()

        frame[1] =
            (message.size and 0xff)
                .toByte()

        message.copyInto(
            destination = frame,
            destinationOffset = HEADER_SIZE
        )

        return frame
    }

    fun chunk(
        frame: ByteArray,
        maxChunkSize: Int
    ): List<ByteArray> {

        require(maxChunkSize > 0) {
            "maxChunkSize must be > 0"
        }

        if (frame.isEmpty()) {
            return emptyList()
        }

        val result =
            mutableListOf<ByteArray>()

        var offset = 0

        while (offset < frame.size) {

            val end =
                minOf(
                    offset + maxChunkSize,
                    frame.size
                )

            result +=
                frame.copyOfRange(
                    offset,
                    end
                )

            offset = end
        }

        return result
    }
}


class TeslaBleFrameDecoder(
    private val maxMessageSize: Int =
        TeslaBleFrameCodec.MAX_MESSAGE_SIZE
) {

    private var buffer =
        ByteArray(0)

    fun feed(
        bytes: ByteArray
    ): List<ByteArray> {

        if (bytes.isEmpty()) {
            return emptyList()
        }

        buffer += bytes

        val messages =
            mutableListOf<ByteArray>()

        var offset = 0

        while (
            buffer.size - offset >=
            TeslaBleFrameCodec.HEADER_SIZE
        ) {

            val high =
                buffer[offset]
                    .toInt() and 0xff

            val low =
                buffer[offset + 1]
                    .toInt() and 0xff

            val messageLength =
                (high shl 8) or low

            require(
                messageLength <=
                    maxMessageSize
            ) {
                "Tesla BLE frame exceeds limit: $messageLength"
            }

            val frameLength =
                TeslaBleFrameCodec.HEADER_SIZE +
                    messageLength

            if (
                buffer.size - offset <
                frameLength
            ) {
                break
            }

            val messageStart =
                offset +
                    TeslaBleFrameCodec.HEADER_SIZE

            val messageEnd =
                messageStart +
                    messageLength

            messages +=
                buffer.copyOfRange(
                    messageStart,
                    messageEnd
                )

            offset += frameLength
        }

        if (offset > 0) {

            buffer =
                if (offset == buffer.size) {
                    ByteArray(0)
                }
                else {
                    buffer.copyOfRange(
                        offset,
                        buffer.size
                    )
                }
        }

        return messages
    }

    fun reset() {
        buffer = ByteArray(0)
    }

    fun bufferedByteCount(): Int =
        buffer.size
}
