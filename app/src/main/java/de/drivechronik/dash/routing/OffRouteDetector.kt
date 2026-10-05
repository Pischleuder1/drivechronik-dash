package de.drivechronik.dash.routing

class OffRouteDetector {
    var confirmed = false
        private set
    private var misses = 0
    private var firstMissMillis = 0L

    fun reset() {
        misses = 0
        firstMissMillis = 0L
        confirmed = false
    }

    fun update(
        route: OfflineRoute,
        position: RoutePoint,
        nowMillis: Long
    ): Boolean {
        val progress = calculateRouteProgress(route, position)
        val distance = progress?.distanceFromRouteMeters

        if (distance == null || !distance.isFinite()) {
            reset()
            return false
        }

        if (distance <= 80.0) {
            reset()
            return false
        }

        if (misses == 0 || nowMillis < firstMissMillis) {
            misses = 1
            firstMissMillis = nowMillis
            return false
        }

        misses++

        confirmed = misses >= 3 &&
            nowMillis - firstMissMillis >= 5000L
        return confirmed
    }
}
