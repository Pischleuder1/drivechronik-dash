package de.drivechronik.dash.routing

import android.content.Context
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import de.drivechronik.dash.OfflineMapRepository
import de.drivechronik.dash.OfflineStartupGate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Ermittelt ein OSM-basiertes Tempolimit aus einer kurzen,
 * zeitlich geordneten GPS-Spur.
 *
 * Kein Wert bedeutet: unbekannt oder nicht verifizierbar.
 * Die Funktion startet keine BLE-Anfrage.
 */
internal object OfflineSpeedLimit {

    fun buildRequest(
        coordinates: List<Pair<Double, Double>>
    ): String {
        require(coordinates.size in 2..12)
        require(coordinates.all { (lat, lon) ->
            lat.isFinite() && lon.isFinite() &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
        })

        return JSONObject().apply {
            put("shape", JSONArray().apply {
                coordinates.forEach { (lat, lon) ->
                    put(
                        JSONObject()
                            .put("lat", lat)
                            .put("lon", lon)
                    )
                }
            })
            put("costing", "auto")
            put("shape_match", "map_snap")
            put("filters", JSONObject().apply {
                put("action", "include")
                put("attributes", JSONArray().apply {
                    put("edge.speed_limit")
                    put("edge.length")
                    put("matched.point")
                    put("matched.edge_index")
                    put("matched.type")
                    put("matched.distance_from_trace_point")
                })
            })
        }.toString()
    }

    fun parseLastSpeedLimit(response: String): Int? {
        val root = JSONObject(response)

        val edges = root.optJSONArray("edges") ?: return null
        val matches = root.optJSONArray("matched_points")
            ?: return null

        if (edges.length() == 0 || matches.length() < 2) {
            return null
        }

        // Ausschließlich den neuesten GPS-Punkt verwenden.
        val match = matches.optJSONObject(matches.length() - 1)
            ?: return null

        if (match.optString("type") != "matched") {
            return null
        }

        // Eine weit entfernte Straße nicht als Treffer werten.
        val distance = match.optDouble(
            "distance_from_trace_point",
            Double.NaN
        )

        if (!distance.isFinite() || distance !in 0.0..25.0) {
            return null
        }

        if (!match.has("edge_index")) {
            return null
        }

        val index = match.optInt("edge_index", -1)

        if (index !in 0 until edges.length()) {
            return null
        }

        val edge = edges.optJSONObject(index) ?: return null

        if (!edge.has("speed_limit") ||
            edge.isNull("speed_limit")) {
            return null
        }

        val limit = edge.optDouble(
            "speed_limit",
            Double.NaN
        )

        if (!limit.isFinite() || limit !in 5.0..200.0) {
            return null
        }

        return limit.toInt()
    }

    suspend fun lookup(
        context: Context,
        coordinates: List<Pair<Double, Double>>
    ): Int? = withContext(Dispatchers.IO) {
        if (!OfflineStartupGate.routingReady) {
            return@withContext null
        }

        runCatching {
            val activePackage =
                OfflineMapRepository.activePackage(context)

            val tiles = activePackage.routingFile
                ?: return@runCatching null

            if (!tiles.isFile || tiles.length() == 0L) {
                return@runCatching null
            }

            val config =
                ValhallaConfigFactory.usingTileExtract(
                    tiles.absolutePath
                )

            val request = buildRequest(coordinates)

            Valhalla(context, config).use { engine ->
                parseLastSpeedLimit(
                    engine.traceAttributesRaw(request)
                )
            }
        }.getOrNull()
    }
}
