package de.drivechronik.dash

import android.os.SystemClock
import androidx.compose.runtime.*
import de.drivechronik.dash.routing.*
import kotlin.math.*

internal object NavigationSimulation {
    var active by mutableStateOf(false)
        private set

    var paused by mutableStateOf(false)
        private set

    var gpsOutage by mutableStateOf(false)
        private set

    var speedKmh by mutableIntStateOf(60)
        private set

    var generation by mutableIntStateOf(0)
        private set

    var fix by mutableStateOf<NavigationGpsFix?>(null)
        private set

    private var route: OfflineRoute? = null
    private var index = 0
    private var fraction = 0.0


    fun startDetour(value: OfflineRoute): Boolean {
        if (!active || gpsOutage ||
            value.points.size < 2) return false

        route = value
        index = 0
        fraction = 0.0
        paused = false
        generation++
        publish()
        return true
    }

    fun start(value: OfflineRoute) {
        if (value.points.size < 2) return
        route = value
        index = 0
        fraction = 0.0
        speedKmh = 60
        paused = false
        gpsOutage = false
        active = true
        generation++
        publish()
    }


    fun followReroute(value: OfflineRoute) {
        if (!active || value.points.size < 2) return
        route = value
        index = 0
        fraction = 0.0
        gpsOutage = false
        paused = false
        generation++
        publish()
    }

    fun stop() {
        active = false
        paused = false
        gpsOutage = false
        fix = null
        route = null
        generation++
    }

    fun toggleGpsOutage() {
        if (active) gpsOutage = !gpsOutage
    }

    fun togglePause() {
        if (active) paused = !paused
    }

    fun changeSpeed() {
        speedKmh = when (speedKmh) {
            30 -> 60
            60 -> 90
            else -> 30
        }
    }


    fun advance(seconds: Double) {
        val points = route?.points ?: return
        if (!active) return
        if (paused) {
            publish()
            return
        }
        var remaining = speedKmh / 3.6 * seconds

        while (remaining > 0.0 && index < points.size - 1) {
            val length = segmentLength(
                points[index], points[index + 1]
            )
            if (length < 0.01) {
                index++
                fraction = 0.0
                continue
            }
            val available = length * (1.0 - fraction)
            if (remaining < available) {
                fraction += remaining / length
                remaining = 0.0
            } else {
                remaining -= available
                index++
                fraction = 0.0
            }
        }
        if (index >= points.size - 1) {
            index = points.size - 2
            fraction = 1.0
            paused = true
        }
        publish()
    }



    fun jumpToNextTurn(): Boolean {
        val r = route ?: return false
        if (!active || gpsOutage) return false

        val points = r.points
        val distances = DoubleArray(points.size)

        for (i in 1 until points.size) {
            distances[i] = distances[i - 1] +
                segmentLength(points[i - 1], points[i])
        }

        val current = distances[index] +
            (distances[index + 1] - distances[index]) *
            fraction

        val turns = setOf(
            9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20, 21, 22, 23, 24, 27
        )

        val next = r.maneuvers
            .filter {
                it.type in turns &&
                it.beginShapeIndex in points.indices
            }
            .map { distances[it.beginShapeIndex] }
            .firstOrNull { it > current + 30.0 }
            ?: return false

        val target = maxOf(current + 10.0, next - 150.0)
        if (target >= next) return false

        var i = index
        while (i < points.size - 2 &&
            distances[i + 1] < target) {
            i++
        }

        index = i
        val length = distances[i + 1] - distances[i]
        fraction = if (length > 0.0) {
            ((target - distances[i]) / length)
                .coerceIn(0.0, 1.0)
        } else 0.0

        publish()
        generation++
        return true
    }

    private fun publish() {
        val points = route?.points ?: return
        val a = points[index]
        val b = points[index + 1]
        val lat = a.lat + (b.lat - a.lat) * fraction
        val lon = a.lon + (b.lon - a.lon) * fraction
        val heading = (
            Math.toDegrees(atan2(
                (b.lon - a.lon) * cos(Math.toRadians(lat)),
                b.lat - a.lat
            )) + 360.0
        ) % 360.0
        fix = NavigationGpsFix(
            RoutePoint(lat, lon),
            SystemClock.elapsedRealtimeNanos(),
            "simulator",
            heading.toFloat()
        )
    }

    private fun segmentLength(
        a: RoutePoint, b: RoutePoint
    ): Double {
        val lat1 = Math.toRadians(a.lat)
        val lat2 = Math.toRadians(b.lat)
        val dLat = lat2 - lat1
        val dLon = Math.toRadians(b.lon - a.lon)
        val h = sin(dLat / 2).pow(2) +
            cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
        return 12_742_000.0 * asin(
            sqrt(h.coerceIn(0.0, 1.0))
        )
    }

}
