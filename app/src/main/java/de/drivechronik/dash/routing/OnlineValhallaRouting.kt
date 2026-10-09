package de.drivechronik.dash.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Online-Valhalla für Routingoptionen, die der öffentliche
 * OSRM-Dienst nicht zuverlässig unterstützt.
 *
 * Aktuell insbesondere:
 * - Autobahn möglichst vermeiden
 *
 * Es werden keinerlei Offline-Routingdaten benötigt.
 */
suspend fun calculateOnlineValhallaRoute(
    start: RoutePoint,
    end: RoutePoint,
    startHeading: Float? = null
): Result<OfflineRoute> = withContext(Dispatchers.IO) {

    runCatching {
        require(
            start.lat.isFinite() &&
                start.lon.isFinite() &&
                end.lat.isFinite() &&
                end.lon.isFinite()
        )

        val request = JSONObject().apply {
            put(
                "locations",
                JSONArray().apply {
                    put(
                        JSONObject()
                            .put("lat", start.lat)
                            .put("lon", start.lon)
                            .apply {
                                startHeading
                                    ?.takeIf { it.isFinite() }
                                    ?.let {
                                        put(
                                            "heading",
                                            it.toInt().mod(360)
                                        )
                                        put(
                                            "heading_tolerance",
                                            45
                                        )
                                    }
                            }
                    )

                    put(
                        JSONObject()
                            .put("lat", end.lat)
                            .put("lon", end.lon)
                    )
                }
            )

            put("costing", "auto")

            put(
                "costing_options",
                JSONObject().apply {
                    put(
                        "auto",
                        JSONObject().apply {
                            put("use_highways", 0.0)
                        }
                    )
                }
            )

            put(
                "directions_options",
                JSONObject().apply {
                    put("units", "kilometers")
                    put("language", "de-DE")
                }
            )
        }

        val url =
            URL("https://valhalla1.openstreetmap.de/route")

        val connection =
            url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000

            connection.setRequestProperty(
                "Content-Type",
                "application/json"
            )
            connection.setRequestProperty(
                "Accept",
                "application/json"
            )
            connection.setRequestProperty(
                "User-Agent",
                "DriveChronik-Dash/0.4.0 Android"
            )
            connection.setRequestProperty(
                "X-Client-Id",
                "drivechronik-dash"
            )

            connection.outputStream.use {
                it.write(
                    request.toString()
                        .toByteArray(Charsets.UTF_8)
                )
            }

            val responseCode = connection.responseCode

            if (responseCode !in 200..299) {
                val errorBody =
                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }

                error(
                    "Online-Valhalla HTTP $responseCode" +
                        if (!errorBody.isNullOrBlank())
                            ": $errorBody"
                        else
                            ""
                )
            }

            val response =
                connection.inputStream
                    .bufferedReader()
                    .use {
                        JSONObject(it.readText())
                    }

            val trip =
                response.getJSONObject("trip")

            val summary =
                trip.getJSONObject("summary")

            val legs =
                trip.getJSONArray("legs")

            val points =
                mutableListOf<RoutePoint>()

            val maneuvers =
                mutableListOf<RouteManeuver>()

            for (i in 0 until legs.length()) {
                val leg =
                    legs.getJSONObject(i)

                val offset =
                    if (points.isEmpty())
                        0
                    else
                        points.size - 1

                val decoded =
                    decodeOnlineValhallaPolyline6(
                        leg.getString("shape")
                    )

                leg.optJSONArray("maneuvers")
                    ?.let { list ->
                        for (j in 0 until list.length()) {
                            val m =
                                list.getJSONObject(j)

                            maneuvers +=
                                RouteManeuver(
                                    instruction =
                                        m.optString(
                                            "instruction"
                                        ),
                                    type =
                                        m.optInt("type"),
                                    distanceKm =
                                        m.optDouble(
                                            "length",
                                            0.0
                                        ),
                                    durationSeconds =
                                        m.optDouble(
                                            "time",
                                            0.0
                                        ),
                                    beginShapeIndex =
                                        offset +
                                            m.getInt(
                                                "begin_shape_index"
                                            ),
                                    endShapeIndex =
                                        offset +
                                            m.getInt(
                                                "end_shape_index"
                                            )
                                )
                        }
                    }

                points.addAll(
                    if (points.isNotEmpty())
                        decoded.drop(1)
                    else
                        decoded
                )
            }

            require(points.size >= 2) {
                "Online-Valhalla lieferte keine Routengeometrie"
            }

            OfflineRoute(
                points = points,
                distanceKm =
                    summary.getDouble("length"),
                durationSeconds =
                    summary.getDouble("time"),
                maneuvers = maneuvers,
                destination = end
            )
        } finally {
            connection.disconnect()
        }
    }
}

private fun decodeOnlineValhallaPolyline6(
    encoded: String
): List<RoutePoint> {

    val result =
        mutableListOf<RoutePoint>()

    var index = 0
    var lat = 0L
    var lon = 0L

    fun nextDelta(): Long {
        var value = 0L
        var shift = 0

        while (true) {
            require(
                index < encoded.length &&
                    shift <= 60
            ) {
                "Ungültige Valhalla-Polyline"
            }

            val byte =
                encoded[index++].code - 63

            require(byte in 0..63)

            value =
                value or
                    ((byte and 31).toLong() shl shift)

            if (byte < 32)
                break

            shift += 5
        }

        return if (
            (value and 1L) != 0L
        ) {
            (value shr 1).inv()
        } else {
            value shr 1
        }
    }

    while (index < encoded.length) {
        lat += nextDelta()
        lon += nextDelta()

        result +=
            RoutePoint(
                lat / 1_000_000.0,
                lon / 1_000_000.0
            )
    }

    return result
}
