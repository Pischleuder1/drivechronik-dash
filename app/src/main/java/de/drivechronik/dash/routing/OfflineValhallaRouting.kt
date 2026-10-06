package de.drivechronik.dash.routing

import android.content.Context
import de.drivechronik.dash.NavigationHighwaySetting
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

data class RouteManeuver(
    val instruction: String,
    val type: Int,
    val distanceKm: Double,
    val durationSeconds: Double,
    val beginShapeIndex: Int,
    val endShapeIndex: Int
)

data class OfflineRoute(
    val points: List<RoutePoint>,
    val distanceKm: Double,
    val durationSeconds: Double,
    val maneuvers: List<RouteManeuver>,
    val destination: RoutePoint? = null
)

suspend fun calculateOfflineRoute(
    context: Context,
    start: RoutePoint,
    end: RoutePoint,
    startHeading: Float? = null
): Result<OfflineRoute> = withContext(Dispatchers.IO) {
    runCatching {
        val tiles = File(
            context.filesDir, "routing/valhalla_tiles.tar"
        )
        require(tiles.isFile && tiles.length() > 0L) {
            "Offline-Routingdaten fehlen"
        }

        val request = JSONObject().apply {
            put("locations", org.json.JSONArray().apply {
                put(JSONObject()
                    .put("lat", start.lat)
                    .put("lon", start.lon)
                    .apply {
                        startHeading
                            ?.takeIf { it.isFinite() }
                            ?.let {
                                put("heading", it.toInt().mod(360))
                                put("heading_tolerance", 45)
                            }
                    })
                put(JSONObject().put("lat", end.lat).put("lon", end.lon))
            })
            put("costing", "auto")
            if (NavigationHighwaySetting.enabled(context)) {
                put("costing_options", JSONObject().apply {
                    put("auto", JSONObject().apply {
                        put("use_highways", 0.0)
                    })
                })
            }
            put("directions_options", JSONObject().apply {
                put("units", "kilometers")
                put("language", "de-DE")
            })
        }

        val config = ValhallaConfigFactory
            .usingTileExtract(tiles.absolutePath)

        val response = Valhalla(context, config).use { engine ->
            JSONObject(engine.routeRaw(request.toString()))
        }

        val trip = response.getJSONObject("trip")
        val summary = trip.getJSONObject("summary")
        val legs = trip.getJSONArray("legs")
        val points = mutableListOf<RoutePoint>()
        val maneuvers = mutableListOf<RouteManeuver>()

        for (i in 0 until legs.length()) {
            val leg = legs.getJSONObject(i)
            val offset = if (points.isEmpty()) 0 else points.size - 1
            val decoded = decodePolyline6(leg.getString("shape"))

            leg.optJSONArray("maneuvers")?.let { list ->
                for (j in 0 until list.length()) {
                    val m = list.getJSONObject(j)
                    maneuvers += RouteManeuver(
                        m.optString("instruction"),
                        m.optInt("type"),
                        m.optDouble("length", 0.0),
                        m.optDouble("time", 0.0),
                        offset + m.getInt("begin_shape_index"),
                        offset + m.getInt("end_shape_index")
                    )
                }
            }
            points.addAll(
                if (points.isNotEmpty()) decoded.drop(1)
                else decoded
            )
        }

        require(points.size >= 2) {
            "Keine Routengeometrie"
        }

        OfflineRoute(
            points = points,
            distanceKm = summary.getDouble("length"),
            durationSeconds = summary.getDouble("time"),
            maneuvers = maneuvers,
            destination = end
        )
    }
}

private fun decodePolyline6(encoded: String): List<RoutePoint> {
    val result = mutableListOf<RoutePoint>()
    var index = 0
    var lat = 0L
    var lon = 0L

    fun nextDelta(): Long {
        var value = 0L
        var shift = 0

        while (true) {
            require(index < encoded.length && shift <= 60) {
                "Ungueltige Polyline"
            }
            val byte = encoded[index++].code - 63
            require(byte in 0..63)
            value = value or ((byte and 31).toLong() shl shift)
            if (byte < 32) break
            shift += 5
        }

        return if ((value and 1L) != 0L) {
            (value shr 1).inv()
        } else {
            value shr 1
        }
    }

    while (index < encoded.length) {
        lat += nextDelta()
        lon += nextDelta()
        result += RoutePoint(
            lat / 1_000_000.0,
            lon / 1_000_000.0
        )
    }

    return result
}
