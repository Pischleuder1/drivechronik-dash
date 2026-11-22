package de.drivechronik.dash.routing

import kotlin.math.cos
import kotlin.math.hypot

/**
 * Kurze, zeitlich geordnete GPS-Spur.
 *
 * Nur für die Zuordnung zu OSM-Straßendaten.
 * Keine BLE-Anfragen und keine Netzwerkzugriffe.
 */
internal class SpeedLimitGpsTrace {

    private data class Sample(
        val lat: Double,
        val lon: Double,
        val timeMs: Long
    )

    private val samples = ArrayDeque<Sample>()

    fun reset() {
        samples.clear()
    }

    fun add(
        lat: Double,
        lon: Double,
        timeMs: Long
    ): Boolean {
        if (!lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) {
            reset()
            return false
        }

        val previous = samples.lastOrNull()

        if (previous != null) {
            val elapsed = timeMs - previous.timeMs

            if (elapsed <= 0L) {
                return false
            }

            if (elapsed > 10_000L) {
                reset()
            } else {
                val distance = distanceMeters(
                    previous.lat, previous.lon,
                    lat, lon
                )

                // Unveränderte GPS-Position nicht doppelt speichern.
                if (distance < 3.0) {
                    return false
                }

                // Unplausiblen Positionssprung verwerfen.
                if (distance > 60.0 + elapsed * 0.05) {
                    reset()
                    return false
                }
            }
        }

        samples.addLast(Sample(lat, lon, timeMs))

        while (samples.size > 6) {
            samples.removeFirst()
        }

        return true
    }

    fun coordinates(): List<Pair<Double, Double>> {
        if (samples.size < 2) return emptyList()

        val first = samples.first()
        val last = samples.last()

        if (last.timeMs - first.timeMs > 15_000L) {
            return emptyList()
        }

        val distance = distanceMeters(
            first.lat, first.lon,
            last.lat, last.lon
        )

        if (distance < 12.0) return emptyList()

        return samples.map { it.lat to it.lon }
    }

    private fun distanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val meanLatitude = Math.toRadians(
            (lat1 + lat2) / 2.0
        )

        return 6_371_000.0 * hypot(
            Math.toRadians(lon2 - lon1) *
                cos(meanLatitude),
            Math.toRadians(lat2 - lat1)
        )
    }
}
