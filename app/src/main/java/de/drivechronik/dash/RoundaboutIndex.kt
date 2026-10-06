package de.drivechronik.dash

import android.content.Context
import de.drivechronik.dash.routing.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.math.*

internal data class RoundaboutMatch(
    val id: Long,
    val ring: List<RoutePoint>,
    val entries: List<Int>,
    val exits: List<Int>,
    val distanceMeters: Double
)

internal object RoundaboutIndex {

    private fun distance(
        a: RoutePoint,
        b: RoutePoint
    ): Double {
        val lat = Math.toRadians((a.lat + b.lat) / 2)
        val dx = Math.toRadians(b.lon - a.lon) * cos(lat)
        val dy = Math.toRadians(b.lat - a.lat)

        return 6371000.0 * hypot(dx, dy)
    }

    suspend fun find(
        context: Context,
        position: RoutePoint
    ): RoundaboutMatch? = withContext(Dispatchers.IO) {

        var best: RoundaboutMatch? = null
        var secondDistance = Double.POSITIVE_INFINITY

        context.assets.open(
            "roundabouts/roundabout_index.jsonl"
        ).bufferedReader().useLines { lines ->

            for (line in lines) {
                val obj = JSONObject(line)
                val points = obj.getJSONArray("ring")

                if (points.length() < 3) continue

                val first = points.getJSONArray(0)
                val approximate = RoutePoint(
                    first.getDouble(1),
                    first.getDouble(0)
                )

                // Grobe Vorfilterung.
                if (distance(position, approximate) > 250.0) {
                    continue
                }

                val ring = ArrayList<RoutePoint>()

                for (i in 0 until points.length()) {
                    val xy = points.getJSONArray(i)

                    ring.add(
                        RoutePoint(
                            xy.getDouble(1),
                            xy.getDouble(0)
                        )
                    )
                }

                val nearest = ring.minOf {
                    distance(position, it)
                }

                if (nearest > 40.0) continue

                val entries = obj.getJSONArray("entries")
                val exits = obj.getJSONArray("exits")

                fun indices(
                    array: org.json.JSONArray
                ) = List(array.length()) {
                    array.getInt(it)
                }

                val candidate = RoundaboutMatch(
                    id = obj.getLong("id"),
                    ring = ring,
                    entries = indices(entries),
                    exits = indices(exits),
                    distanceMeters = nearest
                )

                if (nearest <
                    (best?.distanceMeters ?: Double.POSITIVE_INFINITY)
                ) {
                    secondDistance =
                        best?.distanceMeters
                            ?: Double.POSITIVE_INFINITY
                    best = candidate
                } else {
                    secondDistance = min(
                        secondDistance, nearest
                    )
                }
            }
        }

        val selected = best ?: return@withContext null

        // Bei benachbarten oder überlappenden
        // Kreisverkehren keinen Treffer erzwingen.
        if (secondDistance - selected.distanceMeters < 20.0) {
            return@withContext null
        }

        selected
    }
}
