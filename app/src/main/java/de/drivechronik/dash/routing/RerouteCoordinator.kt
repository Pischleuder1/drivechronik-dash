package de.drivechronik.dash.routing

import kotlinx.coroutines.ensureActive

class RerouteCoordinator(
    initialRoute: OfflineRoute,
    private val calculate: suspend (
        RoutePoint, RoutePoint
    ) -> Result<OfflineRoute>
) {
    var route = initialRoute
        private set

    var attempts = 0
        private set

    private var generation = 0L
    private val detector = OffRouteDetector()

    val isOffRoute: Boolean
        get() = detector.confirmed

    fun adoptRoute(newRoute: OfflineRoute) {
        if (route !== newRoute) {
            generation++
            route = newRoute
            detector.reset()
        }
    }

    fun resetDetection() {
        detector.reset()
    }
    private val cooldown = RerouteCooldown()

    suspend fun onFix(
        position: RoutePoint,
        nowMillis: Long,
        headingDegrees: Float? = null
    ): Boolean {
        if (!detector.update(route, position, nowMillis, headingDegrees)) {
            return false
        }
        if (!cooldown.canAttempt(nowMillis)) return false

        cooldown.markAttempt(nowMillis)
        attempts++
        val requestedGeneration = generation

        val target = route.destination
            ?: route.points.last()

        val result = runCatching {
            calculate(position, target)
        }.getOrElse { Result.failure(it) }

        // Nach einem Navigationsabbruch keine
        // verspäteten Valhalla-Ergebnisse übernehmen.
        kotlinx.coroutines.currentCoroutineContext()
            .ensureActive()

        if (generation != requestedGeneration) return false
        val selected = selectReroutedRoute(route, result)
        if (selected === route) return false

        generation++
        route = selected
        detector.reset()
        return true
    }
}
