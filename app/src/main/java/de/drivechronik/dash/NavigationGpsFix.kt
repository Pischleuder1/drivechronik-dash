package de.drivechronik.dash

import android.location.Location
import android.os.SystemClock
import de.drivechronik.dash.data.TeslaDashboardRepository
import de.drivechronik.dash.routing.RoutePoint


internal fun navigationGpsIsFresh(
    timestamp: Long,
    now: Long
): Boolean =
    now >= timestamp &&
    now - timestamp <= 8_000_000_000L

internal data class NavigationGpsFix(
    val point: RoutePoint,
    val timestampNanos: Long,
    val source: String,
    val heading: Float?
)

internal fun selectNavigationGpsFix(
    tesla: NavigationGpsFix?,
    phone: NavigationGpsFix?
): NavigationGpsFix? = tesla ?: phone

internal fun currentNavigationFix(
    phone: Location?
): NavigationGpsFix? {
    if (NavigationSimulation.active) {
        return if (NavigationSimulation.gpsOutage) {
            null
        } else {
            NavigationSimulation.fix
        }
    }

    var teslaFix: NavigationGpsFix? = null
    var phoneFix: NavigationGpsFix? = null
    val snapshot = TeslaDashboardRepository.snapshot.value
    val timestamp = snapshot.locationUpdatedAtNanos
    val location = snapshot.location

    if (timestamp != null && location != null) {
        val age = System.nanoTime() - timestamp
        val lat = location.latitude?.toDouble()
        val lon = location.longitude?.toDouble()

        if (navigationGpsIsFresh(timestamp, System.nanoTime()) &&
            lat != null && lon != null &&
            lat.isFinite() && lon.isFinite() &&
            lat in -90.0..90.0 &&
            lon in -180.0..180.0
        ) {
            teslaFix = NavigationGpsFix(
                RoutePoint(lat, lon),
                timestamp,
                "tesla",
                null
            )
        }
    }

    if (phone != null) {
        val age = SystemClock.elapsedRealtimeNanos() -
            phone.elapsedRealtimeNanos

        if (navigationGpsIsFresh(phone.elapsedRealtimeNanos, SystemClock.elapsedRealtimeNanos()) &&
            phone.latitude in -90.0..90.0 &&
            phone.longitude in -180.0..180.0 &&
            (!phone.hasAccuracy() ||
                phone.accuracy <= 40f)
        ) {
            phoneFix = NavigationGpsFix(
                RoutePoint(phone.latitude, phone.longitude),
                phone.elapsedRealtimeNanos,
                "phone",
                phone.takeIf {
                    it.hasSpeed() && it.speed >= 1.5f &&
                    it.hasBearing()
                }?.bearing
            )
        }
    }

    return selectNavigationGpsFix(teslaFix, phoneFix)
}
