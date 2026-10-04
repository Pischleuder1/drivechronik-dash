package de.drivechronik.dash.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class RoutePoint(val lat: Double, val lon: Double)

data class RouteStep(
    val road: String,
    val type: String,
    val direction: String,
    val distanceM: Double,
    val point: RoutePoint
)

data class DrivingRoute(
    val distanceM: Double,
    val durationS: Double,
    val points: List<RoutePoint>,
    val steps: List<RouteStep>
)

suspend fun calculateRoute(
    start: RoutePoint,
    destination: RoutePoint
): DrivingRoute? = withContext(Dispatchers.IO) {
    val valid = listOf(start, destination).all {
        it.lat.isFinite() && it.lon.isFinite() &&
        it.lat in -90.0..90.0 && it.lon in -180.0..180.0
    }
    if (!valid) return@withContext null

    val url = String.format(
        Locale.US,
        "https://router.project-osrm.org/route/v1/driving/" +
            "%.6f,%.6f;%.6f,%.6f" +
            "?overview=full&geometries=geojson&steps=true",
        start.lon, start.lat, destination.lon, destination.lat
    )

    var connection: HttpURLConnection? = null
    try {
        connection = URL(url).openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        if (connection.responseCode != 200) {
            android.util.Log.w(
                "DashRouting",
                "OSRM HTTP " + connection.responseCode
            )
            return@withContext null
        }

        val json = connection.inputStream.bufferedReader()
            .use { JSONObject(it.readText()) }

        if (json.optString("code") != "Ok") {
            android.util.Log.w(
                "DashRouting",
                "OSRM code: " + json.optString("code")
            )
        }
        if (json.optString("code") != "Ok")
            return@withContext null

        val route = json.getJSONArray("routes").getJSONObject(0)
        val coords = route.getJSONObject("geometry")
            .getJSONArray("coordinates")

        val points = (0 until coords.length()).map { i ->
            val pair = coords.getJSONArray(i)
            RoutePoint(pair.getDouble(1), pair.getDouble(0))
        }

        val steps = mutableListOf<RouteStep>()
        val legs = route.getJSONArray("legs")
        for (i in 0 until legs.length()) {
            val legSteps = legs.getJSONObject(i).getJSONArray("steps")
            for (j in 0 until legSteps.length()) {
                val step = legSteps.getJSONObject(j)
                val maneuver = step.getJSONObject("maneuver")
                val loc = maneuver.getJSONArray("location")
                steps += RouteStep(
                    step.optString("name"),
                    maneuver.optString("type"),
                    maneuver.optString("modifier"),
                    step.getDouble("distance"),
                    RoutePoint(loc.getDouble(1), loc.getDouble(0))
                )
            }
        }

        DrivingRoute(
            route.getDouble("distance"),
            route.getDouble("duration"),
            points,
            steps
        )
    } catch (e: Exception) {
        android.util.Log.w(
            "DashRouting",
            "Fehler: " + e.javaClass.simpleName
        )
        null
    } finally {
        connection?.disconnect()
    }
}
