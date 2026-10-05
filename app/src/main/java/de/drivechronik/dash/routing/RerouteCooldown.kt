package de.drivechronik.dash.routing

class RerouteCooldown(
    private val intervalMillis: Long = 60_000L
) {
    private var lastAttempt: Long? = null

    fun canAttempt(now: Long): Boolean {
        val last = lastAttempt ?: return true
        return now >= last &&
            now - last >= intervalMillis
    }

    fun markAttempt(now: Long) {
        lastAttempt = now
    }

    fun reset() {
        lastAttempt = null
    }
}
