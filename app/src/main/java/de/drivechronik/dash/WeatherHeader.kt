package de.drivechronik.dash

import android.os.SystemClock
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drivechronik.dash.data.TeslaDashboardRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.HttpURLConnection
import java.util.Locale
import kotlin.math.abs

private data class WeatherNow(
    val temp: Double,
    val wind: Double,
    val code: Int,
    val day: Boolean,
    val lat: Double,
    val lon: Double,
    val updated: Long
)

private suspend fun loadWeather(
    lat: Double,
    lon: Double
): WeatherNow? = withContext(Dispatchers.IO) {
    var conn: HttpURLConnection? = null
    try {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast" +
            "?latitude=%.3f&longitude=%.3f" +
            "&current=temperature_2m,weather_code," +
            "wind_speed_10m,is_day",
            lat, lon
        )
        conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        conn.requestMethod = "GET"

        if (conn.responseCode != 200)
            return@withContext null

        val content = conn.inputStream.bufferedReader()
            .use { it.readText() }
        val json = JSONObject(content).getJSONObject("current")

        WeatherNow(
            json.getDouble("temperature_2m"),
            json.getDouble("wind_speed_10m"),
            json.getInt("weather_code"),
            json.optInt("is_day", 1) == 1,
            lat, lon,
            SystemClock.elapsedRealtime()
        )
    } catch (_: Exception) {
        null
    } finally {
        conn?.disconnect()
    }
}

private fun weatherSymbol(code: Int, day: Boolean): String =
    when (code) {
        0 -> if (day) "☀" else "☾"
        1, 2 -> "⛅"
        3, 45, 48 -> "☁"
        in 51..67, in 80..82 -> "☂"
        in 71..77, 85, 86 -> "❄"
        in 95..99 -> "⚡"
        else -> "◌"
    }

@Composable
fun WeatherHeader() {
    val (phoneState, allowed, requestPermission) =
        rememberPhoneWeatherLocation()

    var weather by remember { mutableStateOf<WeatherNow?>(null) }
    var now by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        var lastAttempt = -60_000L
        while (true) {
            now = SystemClock.elapsedRealtime()
            val gps = weatherCoordinates(phoneState.value)
            val lat = gps?.first
            val lon = gps?.second

            if (lat != null && lon != null &&
                lat.isFinite() && lon.isFinite() &&
                lat in -90.0..90.0 &&
                lon in -180.0..180.0
            ) {
                val old = weather
                val moved = old == null ||
                    abs(lat - old.lat) > 0.05 ||
                    abs(lon - old.lon) > 0.05

                val due = old == null ||
                    now - old.updated >= 600_000L

                if ((moved || due) &&
                    now - lastAttempt >= 60_000L
                ) {
                    lastAttempt = now
                    val result = loadWeather(lat, lon)
                    if (result != null) weather = result
                }
            }
            delay(15_000L)
        }
    }

    val gps = weatherCoordinates(phoneState.value)
    val lat = gps?.first
    val lon = gps?.second
    val data = weather?.takeIf {
        now - it.updated < 1_800_000L &&
        lat != null && lon != null &&
        abs(lat - it.lat) <= 0.05 &&
        abs(lon - it.lon) <= 0.05
    }

    val label = if (data == null) {
        if (!allowed && gps == null) "Standort erlauben" else "Wetter —"
    } else {
        String.format(
            Locale.GERMANY,
            "%s %.0f°C · %.0f km/h",
            weatherSymbol(data.code, data.day),
            data.temp, data.wind
        )
    }

    Text(
        text = label,
        color = Color(0xFFC6D1DC),
        fontSize = 11.sp,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .padding(end = 12.dp)
            .clickable(enabled = !allowed && gps == null) {
                requestPermission()
            }
    )
}
