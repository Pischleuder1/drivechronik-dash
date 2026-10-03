package de.drivechronik.dash.ble

class TeslaResponseReplayGuard {

    private val counters =
        mutableMapOf<
            String,
            MutableSet<Long>
        >()


    @Synchronized
    fun accept(
        requestHash: ByteArray,
        counter: Long
    ): Boolean {

        require(
            requestHash.isNotEmpty()
        )

        require(
            counter in 0..0xffff_ffffL
        )

        val key =
            requestHash.toHexKey()

        val used =
            counters.getOrPut(key) {
                mutableSetOf()
            }

        return used.add(
            counter
        )
    }


    @Synchronized
    fun clear(
        requestHash: ByteArray
    ) {

        counters.remove(
            requestHash.toHexKey()
        )
    }


    @Synchronized
    fun clearAll() {

        counters.clear()
    }


    private fun ByteArray.toHexKey():
        String {

        return joinToString("") {
            "%02x".format(
                it.toInt() and 0xff
            )
        }
    }
}
