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
                        val nearRoute =
                            route.points.any { point ->
                                meters(
                                    point.lat,
                                    point.lon,
                                    event.lat,
                                    event.lon
                                ) <= 500.0
                            }

                        nearRoute &&
                            !isOppositeRouteDirection(
                                event,
                                route.points
                            )
                    }
                    /*
                     * Mehrfachmeldungen derselben Maßnahme
                     * nicht mehrfach in der Navigation zeigen.
                     */
                    .distinctBy { event ->
                        event.id
                    }
                    .let { events ->
                        collapseNearbyRoadworks(
                            events,
                            route.points
                        )
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

                    val geometry =
                        parseTrafficGeometry(item)

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
                                ),
                            geometry = geometry
                        )
                    )
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    /*
     * GeoJSON-Geometrie der Autobahn-API.
     *
     * Reihenfolge der Koordinaten:
     * [longitude, latitude]
     */
    private fun parseTrafficGeometry(
        item: JSONObject
    ): List<RoutePoint> {
        val coordinates =
            item
                .optJSONObject("geometry")
                ?.optJSONArray("coordinates")
                ?: return emptyList()

        val result =
            mutableListOf<RoutePoint>()

        for (i in 0 until coordinates.length()) {
            val pair =
                coordinates.optJSONArray(i)
                    ?: continue

            if (pair.length() < 2) {
                continue
            }

            val lon =
                pair.optDouble(
                    0,
                    Double.NaN
                )

            val lat =
                pair.optDouble(
                    1,
                    Double.NaN
                )

            if (
                lat.isFinite() &&
                lon.isFinite() &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
            ) {
                result +=
                    RoutePoint(
                        lat = lat,
                        lon = lon
                    )
            }
        }

        return result
    }


    private data class RoutePosition(
        val alongMeters: Double,
        val distanceMeters: Double,
        val segmentIndex: Int
    )


    /*
     * Punkt auf die geplante Route projizieren.
     */
    private fun positionOnRoute(
        point: RoutePoint,
        route: List<RoutePoint>
    ): RoutePosition? {
        if (route.size < 2) {
            return null
        }

        var best: RoutePosition? = null
        var travelled = 0.0

        for (i in 0 until route.size - 1) {
            val a = route[i]
            val b = route[i + 1]

            val segmentLength =
                meters(
                    a.lat,
                    a.lon,
                    b.lat,
                    b.lon
                )

            if (segmentLength < 0.01) {
                continue
            }

            val meanLat =
                Math.toRadians(
                    (
                        a.lat +
                            b.lat +
                            point.lat
                        ) / 3.0
                )

            fun x(lon: Double): Double =
                Math.toRadians(lon) *
                    cos(meanLat) *
                    6_371_000.0

            fun y(lat: Double): Double =
                Math.toRadians(lat) *
                    6_371_000.0

            val ax = x(a.lon)
            val ay = y(a.lat)

            val bx = x(b.lon)
            val by = y(b.lat)

            val px = x(point.lon)
            val py = y(point.lat)

            val dx = bx - ax
            val dy = by - ay

            val lengthSquared =
                dx * dx + dy * dy

            if (lengthSquared <= 0.0) {
                travelled += segmentLength
                continue
            }

            val fraction =
                (
                    (
                        (px - ax) * dx +
                            (py - ay) * dy
                    ) / lengthSquared
                ).coerceIn(0.0, 1.0)

            val closestX =
                ax + dx * fraction

            val closestY =
                ay + dy * fraction

            val distance =
                hypot(
                    px - closestX,
                    py - closestY
                )

            val candidate =
                RoutePosition(
                    alongMeters =
                        travelled +
                            segmentLength * fraction,
                    distanceMeters = distance,
                    segmentIndex = i
                )

            if (
                best == null ||
                candidate.distanceMeters <
                    best.distanceMeters
            ) {
                best = candidate
            }

            travelled += segmentLength
        }

        return best
    }


    /*
     * Fahrtrichtung zwischen zwei Punkten.
     */
    private fun bearing(
        a: RoutePoint,
        b: RoutePoint
    ): Double {
        val lat1 =
            Math.toRadians(a.lat)

        val lat2 =
            Math.toRadians(b.lat)

        val dLon =
            Math.toRadians(
                b.lon - a.lon
            )

        val y =
            sin(dLon) * cos(lat2)

        val x =
            cos(lat1) * sin(lat2) -
                sin(lat1) *
                cos(lat2) *
                cos(dLon)

        return (
            Math.toDegrees(
                atan2(y, x)
            ) + 360.0
        ) % 360.0
    }


    private fun angleDifference(
        a: Double,
        b: Double
    ): Double {
        val diff =
            abs(a - b) % 360.0

        return min(
            diff,
            360.0 - diff
        )
    }


    /*
     * Prüft, ob eine Meldung eindeutig zur Gegenfahrbahn
     * gehört.
     *
     * Zwei Verfahren:
     *
     * 1. Lange Ereignisgeometrie:
     *    Anfang und Ende entlang unserer Route positionieren.
     *    Läuft die Geometrie > 150 m rückwärts, wird sie
     *    verworfen.
     *
     * 2. Kürzere Geometrie:
     *    Richtung der Ereignisgeometrie mit dem nächstgelegenen
     *    Routensegment vergleichen.
     *    Ab > 120° Richtungsunterschied gilt sie als
     *    Gegenrichtung.
     *
     * Punktmeldungen oder unklare Geometrien bleiben erhalten.
     */
    private fun isOppositeRouteDirection(
        event: TrafficEvent,
        route: List<RoutePoint>
    ): Boolean {
        val geometry =
            event.geometry

        if (
            geometry.size < 2 ||
            route.size < 2
        ) {
            return false
        }

        val first =
            positionOnRoute(
                geometry.first(),
                route
            )

        val last =
            positionOnRoute(
                geometry.last(),
                route
            )

        /*
         * Verfahren 1:
         * Fortschritt entlang unserer Route.
         */
        if (
            first != null &&
            last != null &&
            first.distanceMeters <= 500.0 &&
            last.distanceMeters <= 500.0
        ) {
            val progress =
                last.alongMeters -
                    first.alongMeters

            if (progress < -150.0) {
                android.util.Log.i(
                    "DriveChronikTraffic",
                    "Gegenrichtung verworfen " +
                        "(Routenfortschritt " +
                        "${progress.toInt()} m): " +
                        "${event.road} ${event.title}"
                )

                return true
            }
        }

        /*
         * Verfahren 2:
         * auch kurze Ereignisgeometrien anhand ihrer
         * tatsächlichen Fahrtrichtung prüfen.
         */
        val geometryStart =
            geometry.first()

        val geometryEnd =
            geometry.last()

        val geometryLength =
            meters(
                geometryStart.lat,
                geometryStart.lon,
                geometryEnd.lat,
                geometryEnd.lon
            )

        /*
         * Unter 30 m ist eine Richtung zu unzuverlässig.
         */
        if (geometryLength < 30.0) {
            return false
        }

        val eventPosition =
            positionOnRoute(
                RoutePoint(
                    lat = event.lat,
                    lon = event.lon
                ),
                route
            ) ?: first ?: return false

        if (eventPosition.distanceMeters > 500.0) {
            return false
        }

        val index =
            eventPosition.segmentIndex
                .coerceIn(
                    0,
                    route.size - 2
                )

        /*
         * Für eine stabilere Routenrichtung möglichst mehrere
         * Punkte um das Ereignis herum benutzen.
         */
        val before =
            (index - 2)
                .coerceAtLeast(0)

        val after =
            (index + 3)
                .coerceAtMost(
                    route.lastIndex
                )

        if (before >= after) {
            return false
        }

        val routeBearing =
            bearing(
                route[before],
                route[after]
            )

        val eventBearing =
            bearing(
                geometryStart,
                geometryEnd
            )

        val difference =
            angleDifference(
                routeBearing,
                eventBearing
            )

        if (difference > 120.0) {
            android.util.Log.i(
                "DriveChronikTraffic",
                "Gegenrichtung verworfen " +
                    "(Winkel ${difference.toInt()}°): " +
                    "${event.road} ${event.title}"
            )

            return true
        }

        return false
    }


    /*
     * Mehrere Baustellenmeldungen derselben Autobahn,
     * die innerhalb von 3 km entlang der gefahrenen
     * Route liegen, zu einem Hinweis zusammenfassen.
     *
     * Andere Warnungen und Sperrungen bleiben unangetastet.
     */
    private fun collapseNearbyRoadworks(
        events: List<TrafficEvent>,
        route: List<RoutePoint>
    ): List<TrafficEvent> {
        if (
            events.isEmpty() ||
            route.size < 2
        ) {
            return events
        }

        data class PositionedRoadwork(
            val event: TrafficEvent,
            val alongMeters: Double
        )

        val positionedRoadworks =
            events
                .filter {
                    it.type ==
                        TrafficEventType.ROADWORK
                }
                .mapNotNull { event ->
                    val position =
                        positionOnRoute(
                            RoutePoint(
                                lat = event.lat,
                                lon = event.lon
                            ),
                            route
                        ) ?: return@mapNotNull null

                    if (
                        position.distanceMeters > 500.0
                    ) {
                        return@mapNotNull null
                    }

                    PositionedRoadwork(
                        event = event,
                        alongMeters =
                            position.alongMeters
                    )
                }
                .sortedBy {
                    it.alongMeters
                }

        val suppressedIds =
            mutableSetOf<String>()

        val roads =
            positionedRoadworks
                .groupBy {
                    it.event.road
                }

        for ((road, roadworks) in roads) {
            var lastKept:
                PositionedRoadwork? = null

            var suppressedCount = 0

            for (roadwork in roadworks) {
                val previous = lastKept

                if (
                    previous != null &&
                    roadwork.alongMeters -
                        previous.alongMeters <= 3_000.0
                ) {
                    suppressedIds +=
                        roadwork.event.id

                    suppressedCount++
                } else {
                    lastKept = roadwork
                }
            }

            if (suppressedCount > 0) {
                android.util.Log.i(
                    "DriveChronikTraffic",
                    "Baustellen zusammengefasst: " +
                        "$road, " +
                        "${roadworks.size} Meldungen, " +
                        "${roadworks.size - suppressedCount} Hinweise"
                )
            }
        }

        return events.filterNot {
            it.id in suppressedIds
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
