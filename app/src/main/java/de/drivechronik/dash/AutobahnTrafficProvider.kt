package de.drivechronik.dash

import de.drivechronik.dash.routing.OfflineRoute
import de.drivechronik.dash.routing.RoutePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.*

internal object AutobahnTrafficProvider {

    private const val BASE =
        "https://verkehr.autobahn.de/o/autobahn"

    suspend fun loadForRoute(
        route: OfflineRoute
    ): Result<List<TrafficEvent>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val roads = motorwayRefs(route)

                android.util.Log.i(
                    "DriveChronikTraffic",
                    "Erkannte Autobahnen: " +
                        if (roads.isEmpty())
                            "<keine>"
                        else
                            roads.sorted().joinToString(", ")
                )

                if (roads.isEmpty()) {
                    return@runCatching emptyList()
                }

                val all = mutableListOf<TrafficEvent>()

                for (road in roads) {
                    all += loadRoadEvents(
                        road,
                        "roadworks",
                        TrafficEventType.ROADWORK
                    )
                    all += loadRoadEvents(
                        road,
                        "warning",
                        TrafficEventType.WARNING
                    )
                    all += loadRoadEvents(
                        road,
                        "closure",
                        TrafficEventType.CLOSURE
                    )
                }

                all
                    .filter { event ->
                        route.points.any { point ->
                            meters(
                                point.lat,
                                point.lon,
                                event.lat,
                                event.lon
                            ) <= 500.0
                        }
                    }
                    /*
                     * Mehrfachmeldungen derselben Maßnahme
                     * nicht mehrfach in der Navigation zeigen.
                     */
                    .distinctBy { event ->
                        event.id
                    }
            }
        }

    private fun motorwayRefs(
        route: OfflineRoute
    ): Set<String> {
        if (route.motorwayRefs.isNotEmpty()) {
            return route.motorwayRefs
        }

        val regex = Regex("""\bA\s?(\d{1,3})\b""")

        return route.maneuvers
            .flatMap { maneuver ->
                regex.findAll(maneuver.instruction)
                    .map { "A${it.groupValues[1]}" }
                    .toList()
            }
            .toSet()
    }

    private fun loadRoadEvents(
        road: String,
        service: String,
        type: TrafficEventType
    ): List<TrafficEvent> {

        val url = URL(
            "$BASE/$road/services/$service"
        )

        val connection =
            url.openConnection() as HttpURLConnection

        return try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "User-Agent",
                "DriveChronik-Dash/0.5.0"
            )

            if (connection.responseCode != 200) {
                return emptyList()
            }

            val root = JSONObject(
                connection.inputStream
                    .bufferedReader()
                    .use { it.readText() }
            )

            val array =
                root.optJSONArray(
                    when (type) {
                        TrafficEventType.ROADWORK ->
                            "roadworks"
                        TrafficEventType.WARNING ->
                            "warning"
                        TrafficEventType.CLOSURE ->
                            "closure"
                    }
                ) ?: return emptyList()

            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i)
                        ?: continue

                    val coordinate =
                        item.optJSONObject("coordinate")
                            ?: continue

                    val lat =
                        coordinate.optDouble(
                            "lat",
                            Double.NaN
                        )
                    val lon =
                        coordinate.optDouble(
                            "long",
                            Double.NaN
                        )

                    if (!lat.isFinite() ||
                        !lon.isFinite()
                    ) continue

                    add(
                        TrafficEvent(
                            id = item.optString(
                                "identifier",
                                "$road-$service-$i"
                            ),
                            type = type,
                            road = road,
                            title = item.optString(
                                "title"
                            ),
                            subtitle =
                                item.optString(
                                    "subtitle"
                                ),
                            lat = lat,
                            lon = lon,
                            blocked =
                                item.optBoolean(
                                    "isBlocked",
                                    type ==
                                        TrafficEventType.CLOSURE
                                )
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun meters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6_371_000.0
        val p1 = Math.toRadians(lat1)
        val p2 = Math.toRadians(lat2)
        val dp = Math.toRadians(lat2 - lat1)
        val dl = Math.toRadians(lon2 - lon1)

        val a =
            sin(dp / 2).pow(2) +
                cos(p1) *
                cos(p2) *
                sin(dl / 2).pow(2)

        return 2 * r *
            atan2(sqrt(a), sqrt(1 - a))
    }
}
