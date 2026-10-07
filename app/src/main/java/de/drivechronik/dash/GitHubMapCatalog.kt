package de.drivechronik.dash

import android.content.Context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class RemoteMapPart(
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
    val url: String
)

internal data class RemoteMapPackage(
    val id: String,
    val country: String,
    val countryName: String,
    val kind: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val fileName: String,
    val parts: List<RemoteMapPart>
)

internal object GitHubMapCatalog {
    private const val URL_CATALOG =
        "https://raw.githubusercontent.com/" +
        "pischleuder1/drivechronik-maps/main/catalog.json"

    private fun readParts(
        item: JSONObject
    ): List<RemoteMapPart> {
        val array = item.getJSONArray("parts")
        require(array.length() > 0)

        return (0 until array.length()).map { index ->
            val part = array.getJSONObject(index)
            val name = part.getString("name")
            val url = part.getString("url")
            val hash = part.getString("sha256")
            val bytes = part.getLong("sizeBytes")

            require(
                name.matches(Regex("[A-Za-z0-9._-]+")) &&
                name != "." && name != ".."
            )
            require(bytes in 1..(2L * 1024 * 1024 * 1024))
            require(hash.matches(Regex("[a-fA-F0-9]{64}")))
            require(
                url.startsWith(
                    "https://github.com/Pischleuder1/" +
                    "drivechronik-maps/releases/download/"
                )
            )

            RemoteMapPart(name, bytes, hash, url)
        }
    }

    suspend fun fetch(context: Context): Result<List<RemoteMapPackage>> =
        withContext(Dispatchers.IO) {
            val cache = context.filesDir.resolve(
                "offline-map-catalog.json"
            )
            runCatching {
                val connection =
                    URL(URL_CATALOG).openConnection()
                        as HttpURLConnection

                try {
                    connection.connectTimeout = 10000
                    connection.readTimeout = 15000
                    connection.setRequestProperty(
                        "Accept", "application/json"
                    )

                    check(connection.responseCode == 200) {
                        "GitHub HTTP ${connection.responseCode}"
                    }

                    val text = connection.inputStream
                        .bufferedReader().use { it.readText() }

                    val json = JSONObject(text)

                    check(json.getInt("schemaVersion") == 1) {
                        "Unbekannte Katalogversion"
                    }

                    val packages = json.getJSONArray("packages")

                    val result = (0 until packages.length()).map { i ->
                        val item = packages.getJSONObject(i)
                        val kind = item.getString("kind")
                        require(kind == "map" || kind == "routing")
                        val size = item.getLong("sizeBytes")
                        require(size > 0L)

                        RemoteMapPackage(
                            id = item.getString("id"),
                            country = item.getString("country"),
                            countryName =
                                item.getString("countryName"),
                            kind = kind,
                            version = item.getString("version"),
                            sizeBytes = size,
                            sha256 = item.getString("sha256"),
                            fileName = item.getString("fileName"),
                            parts = readParts(item)
                        )
                    }

                    val temporary = context.filesDir.resolve(
                        "offline-map-catalog.json.tmp"
                    )
                    temporary.writeText(text)
                    check(temporary.renameTo(cache)) {
                        "Katalog konnte nicht gespeichert werden"
                    }
                    result
                } finally {
                    connection.disconnect()
                }
            }.recoverCatching {
                val cached = JSONObject(cache.readText())
                check(cached.getInt("schemaVersion") == 1)
                val items = cached.getJSONArray("packages")
                (0 until items.length()).map { i ->
                    val item = items.getJSONObject(i)
                    val kind = item.getString("kind")
                    require(kind == "map" || kind == "routing")
                    RemoteMapPackage(
                        id = item.getString("id"),
                        country = item.getString("country"),
                        countryName = item.getString("countryName"),
                        kind = kind,
                        version = item.getString("version"),
                        sizeBytes = item.getLong("sizeBytes"),
                        sha256 = item.getString("sha256"),
                        fileName = item.getString("fileName"),
                            parts = readParts(item)
                    )
                }
            }
        }

    internal fun readCached(
        context: Context
    ): Result<List<RemoteMapPackage>> = runCatching {
        val file = context.filesDir.resolve(
            "offline-map-catalog.json"
        )
        val json = JSONObject(file.readText())
        check(json.getInt("schemaVersion") == 1)

        val items = json.getJSONArray("packages")
        (0 until items.length()).map { i ->
            val item = items.getJSONObject(i)
            val kind = item.getString("kind")
            require(kind == "map" || kind == "routing")
            RemoteMapPackage(
                id = item.getString("id"),
                country = item.getString("country"),
                countryName = item.getString("countryName"),
                kind = kind,
                version = item.getString("version"),
                sizeBytes = item.getLong("sizeBytes"),
                sha256 = item.getString("sha256"),
                fileName = item.getString("fileName"),
                parts = readParts(item)
            )
        }
    }

}
