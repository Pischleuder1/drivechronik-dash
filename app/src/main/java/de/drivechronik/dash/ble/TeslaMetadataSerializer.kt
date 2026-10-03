package de.drivechronik.dash.ble

object TeslaMetadataSerializer {

    const val TAG_END = 0xff

    data class Item(
        val tag: Int,
        val value: ByteArray
    )


    fun serialize(
        items: List<Item>
    ): ByteArray {

        val sorted =
            items.sortedBy {
                it.tag
            }

        val out =
            mutableListOf<Byte>()

        var previousTag = -1

        for (item in sorted) {

            require(
                item.tag in 0..254
            ) {
                "Metadata tag must be 0..254"
            }

            require(
                item.tag > previousTag
            ) {
                "Metadata tags must be unique"
            }

            require(
                item.value.size <= 255
            ) {
                "Metadata value exceeds 255 bytes"
            }

            out +=
                item.tag.toByte()

            out +=
                item.value.size.toByte()

            item.value.forEach {
                out += it
            }

            previousTag =
                item.tag
        }

        out +=
            TAG_END.toByte()

        return out.toByteArray()
    }


    fun uint32(
        value: Long
    ): ByteArray {

        require(
            value in 0..0xffff_ffffL
        )

        return byteArrayOf(
            ((value ushr 24) and 0xff).toByte(),
            ((value ushr 16) and 0xff).toByte(),
            ((value ushr 8) and 0xff).toByte(),
            (value and 0xff).toByte()
        )
    }
}
