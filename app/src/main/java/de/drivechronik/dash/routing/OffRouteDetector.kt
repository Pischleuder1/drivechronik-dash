package de.drivechronik.dash.routing

class OffRouteDetector {
    var confirmed = false
        private set
    private var misses = 0
    private var firstMissMillis = 0L
    private var firstMissPosition: RoutePoint? = null

    fun reset() {
        misses = 0
        firstMissMillis = 0L
        firstMissPosition = null
        confirmed = false
    }

    fun update(
        route: OfflineRoute,
        position: RoutePoint,
        nowMillis: Long,
        headingDegrees: Float? = null
    ): Boolean {
        val progress = calculateRouteProgress(route, position)
        val distance = progress?.distanceFromRouteMeters

        if (distance == null || !distance.isFinite()) {
            reset()
            return false
        }

        // Auch falsche Abzweigungen nahe der Route erkennen.
        val heading = headingDegrees?.takeIf {
            it.isFinite() && it in 0f..360f
        }

        val aligned = heading?.let {
            calculateRouteProgress(
                route, position,
                headingDegrees = it
            )
        }

        val headingMismatch =
            heading != null &&
            distance <= 30.0 &&
            (aligned == null ||
                aligned.distanceFromRouteMeters > 25.0)

        if (distance <= 30.0 && !headingMismatch) {
            reset()
            return false
        }

        if (misses == 0 || nowMillis < firstMissMillis) {
            misses = 1
            firstMissMillis = nowMillis
            firstMissPosition = position
            return false
        }

        misses++

        val movedMeters = firstMissPosition?.let {
            val latitude = Math.toRadians(
                (it.lat + position.lat) / 2.0
            )
            val dx = Math.toRadians(position.lon - it.lon) *
                kotlin.math.cos(latitude)
            val dy = Math.toRadians(position.lat - it.lat)
            6371000.0 * kotlin.math.hypot(dx, dy)
        } ?: 0.0

        confirmed = misses >= 3 &&
            nowMillis - firstMissMillis >= 2000L &&
            (distance > 30.0 ||
                headingMismatch && movedMeters >= 10.0)
        return confirmed
    }
}
