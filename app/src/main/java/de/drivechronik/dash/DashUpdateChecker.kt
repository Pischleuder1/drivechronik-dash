package de.drivechronik.dash

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class DashUpdateInfo(
    val version: String,
    val apkUrl: String,
    val apkName: String,
    val releaseNotes: String,
    val releaseUrl: String
)

internal object DashUpdateChecker {

    private const val API_URL =
        "https://api.github.com/repos/Pischleuder1/drivechronik-dash/releases/latest"

    suspend fun check(currentVersion: String): Result<DashUpdateInfo?> =
        withContext(Dispatchers.IO) {
            runCatching {
                val connection =
                    URL(API_URL).openConnection() as HttpURLConnection

                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.setRequestProperty(
                        "Accept",
                        "application/vnd.github+json"
                    )
                    connection.setRequestProperty(
                        "User-Agent",
                        "DriveChronik-Dash"
                    )

                    if (connection.responseCode != 200) {
                        error(
                            "GitHub HTTP ${connection.responseCode}"
                        )
                    }

                    val json = JSONObject(
                        connection.inputStream.bufferedReader()
                            .use { it.readText() }
                    )

                    val version = json.getString("tag_name")
                        .removePrefix("v")

                    if (!isNewer(version, currentVersion)) {
                        return@runCatching null
                    }

                    val assets = json.getJSONArray("assets")
                    var apkName: String? = null
                    var apkUrl: String? = null

                    for (i in 0 until assets.length()) {
                        val asset = assets.getJSONObject(i)
                        val name = asset.getString("name")

                        if (name == "DriveChronik-Dash-v$version.apk") {
                            apkName = name
                            apkUrl = asset.getString(
                                "browser_download_url"
                            )
                            break
                        }
                    }

                    require(apkName != null && apkUrl != null) {
                        "Keine passende Release-APK gefunden"
                    }

                    require(
                        apkUrl.startsWith(
                            "https://github.com/Pischleuder1/drivechronik-dash/releases/download/"
                        )
                    ) {
                        "Ungültige APK-Downloadadresse"
                    }

                    DashUpdateInfo(
                        version = version,
                        apkUrl = apkUrl,
                        apkName = apkName,
                        releaseNotes = json.optString("body"),
                        releaseUrl = json.getString("html_url")
                    )
                } finally {
                    connection.disconnect()
                }
            }
        }

    private fun isNewer(
        available: String,
        installed: String
    ): Boolean {
        val a = parseVersion(available) ?: return false
        val b = parseVersion(installed) ?: return false

        for (i in 0 until 3) {
            if (a[i] != b[i]) return a[i] > b[i]
        }

        return false
    }

    private fun parseVersion(value: String): List<Int>? {
        val parts = value.removePrefix("v").split(".")
        if (parts.size != 3) return null
        return parts.map { it.toIntOrNull() ?: return null }
    }
}
