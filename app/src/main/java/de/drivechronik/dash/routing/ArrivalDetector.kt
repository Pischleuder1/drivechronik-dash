package de.drivechronik.dash.routing

class ArrivalDetector {
    private var count = 0
    private var since = 0L

    fun reset() {
        count = 0
        since = 0L
    }

    fun update(
        progress: RouteProgress?,
        metersToTarget: Double,
        nowMillis: Long
    ): Boolean {
        val nearEnd =
            progress != null &&
            progress.distanceRemainingMeters <= 20.0 &&
            progress.distanceFromRouteMeters <= 20.0 &&
            metersToTarget.isFinite() &&
            metersToTarget <= 25.0

        if (!nearEnd) {
            reset()
            return false
        }

        if (count == 0 || nowMillis < since) {
            count = 1
            since = nowMillis
            return false
        }

        count++
        return count >= 3 &&
            nowMillis - since >= 2000L
    }
}
