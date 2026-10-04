package de.drivechronik.dash

import android.location.Location
import android.os.SystemClock
import de.drivechronik.dash.data.TeslaDashboardRepository

internal fun weatherCoordinates(phone: Location?): Pair<Double, Double>? {
    val s = TeslaDashboardRepository.snapshot.value
    val age = s.locationUpdatedAtNanos?.let {
        System.nanoTime() - it
    }
    val lat = s.location?.latitude?.toDouble()
    val lon = s.location?.longitude?.toDouble()

    if (age != null && age in 0L..120_000_000_000L &&
        lat != null && lon != null &&
        lat.isFinite() && lon.isFinite() &&
        lat in -90.0..90.0 && lon in -180.0..180.0) {
        return lat to lon
    }

    if (phone != null) {
        val agePhone = SystemClock.elapsedRealtimeNanos() -
            phone.elapsedRealtimeNanos
        if (agePhone in 0L..120_000_000_000L &&
            phone.latitude in -90.0..90.0 &&
            phone.longitude in -180.0..180.0) {
            return phone.latitude to phone.longitude
        }
    }
    return null
}
