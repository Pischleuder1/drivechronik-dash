package de.drivechronik.dash.routing

class RouteTracking {
    private var previous: RouteProgress? = null

    fun reset() {
        previous = null
    }

    fun update(
        route: OfflineRoute,
        position: RoutePoint,
        elapsedSeconds: Double,
        headingDegrees: Float? = null
    ): RouteProgress? {
        val old = previous
        val minimum = (
            (old?.distanceAlongRouteMeters ?: 0.0) - 25.0
        ).coerceAtLeast(0.0)

        val candidate = calculateRouteProgress(
            route, position, minimum, headingDegrees
        ) ?: return null

        // GPS-Position zu weit von der Route entfernt.
        if (candidate.distanceFromRouteMeters > 40.0) {
            return null
        }

        if (old != null) {
            val delta = candidate.distanceAlongRouteMeters -
                old.distanceAlongRouteMeters

            // Begrenzte Rückwärtsbewegung und plausible
            // maximale Vorwärtsbewegung zulassen.
            val maxForward =
                25.0 + elapsedSeconds.coerceIn(0.0, 30.0) * 45.0

            if (delta < -25.0 || delta > maxForward) {
                return null
            }
        }

        previous = candidate
        return candidate
    }
}
