package de.drivechronik.dash.routing

import android.content.Context
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class DestinationResult(
    val name: String,
    val point: RoutePoint
)

@Suppress("DEPRECATION")
suspend fun searchDestination(
    context: Context,
    query: String
): DestinationResult? = withContext(Dispatchers.IO) {
    val text = query.trim()

    if (text.length < 3 || text.length > 200)
        return@withContext null

    if (!Geocoder.isPresent())
        return@withContext null

    try {
        val geocoder = Geocoder(
            context.applicationContext,
            Locale.GERMANY
        )

        val address = geocoder
            .getFromLocationName(text, 1)
            ?.firstOrNull()
            ?: return@withContext null

        val lat = address.latitude
        val lon = address.longitude

        if (!lat.isFinite() || !lon.isFinite() ||
            lat !in -90.0..90.0 ||
            lon !in -180.0..180.0
        ) return@withContext null

        DestinationResult(
            name = address.getAddressLine(0) ?: text,
            point = RoutePoint(lat, lon)
        )
    } catch (_: Exception) {
        null
    }
}
