package de.drivechronik.dash

import de.drivechronik.dash.routing.RoutePoint
import kotlin.math.*

internal class NavigationGpsHandover {
    private var source: String? = null
    private var pending: NavigationGpsFix? = null
    private var lastAccepted: RoutePoint? = null
    private var acceptedAt: Long? = null
    private var acceptedFixTime: Long? = null

    fun reset() {
        source = null
        pending = null
        lastAccepted = null
        acceptedAt = null
        acceptedFixTime = null
    }

    fun accept(
        fix: NavigationGpsFix,
        now: Long = android.os.SystemClock.elapsedRealtimeNanos()
    ): Boolean {
        if (source == null) {
            source = fix.source
            lastAccepted = fix.point
            acceptedAt = now
            acceptedFixTime = fix.timestampNanos
            return true
        }
        if (source == fix.source) {
            pending = null
            if (acceptedFixTime == fix.timestampNanos) {
                return true
            }

            val anchor = lastAccepted ?: return false
            val elapsed = acceptedAt?.let { now - it }
                ?: return false

            if (elapsed < 0L) return false

            val maxDistance = 35.0 +
                elapsed / 1_000_000_000.0 * 50.0

            if (distance(anchor, fix.point) > maxDistance) {
                return false
            }

            lastAccepted = fix.point
            acceptedAt = now
            acceptedFixTime = fix.timestampNanos
            return true
        }

        val previous = pending
        if (previous == null ||
            previous.source != fix.source
        ) {
            pending = fix
            return false
        }

        val dt = fix.timestampNanos -
            previous.timestampNanos

        val distance = distance(
            previous.point, fix.point
        )

        if (dt <= 0L || dt > 8_000_000_000L ||
            distance > 60.0
        ) {
            pending = fix
            return false
        }

        val anchor = lastAccepted ?: return false
        val elapsed = acceptedAt?.let { now - it }
            ?: return false
        if (elapsed < 0L) {
            pending = fix
            return false
        }
        val maxDistance = 35.0 +
            elapsed / 1_000_000_000.0 * 50.0
        if (distance(anchor, fix.point) > maxDistance) {
            pending = fix
            return false
        }

        source = fix.source
        pending = null
        lastAccepted = fix.point
        acceptedAt = now
        acceptedFixTime = fix.timestampNanos
        return true
    }

    private fun distance(
        a: RoutePoint,
        b: RoutePoint
    ): Double {
        val lat = Math.toRadians(
            (a.lat + b.lat) / 2.0
        )
        return 6371000.0 * hypot(
            Math.toRadians(b.lon - a.lon) * cos(lat),
            Math.toRadians(b.lat - a.lat)
        )
    }
}
