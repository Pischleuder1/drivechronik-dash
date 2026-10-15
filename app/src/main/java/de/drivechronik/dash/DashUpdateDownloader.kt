package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

internal object DashUpdateDownloader {

    private const val MAX_APK_BYTES = 500L * 1024L * 1024L

    suspend fun download(
        context: Context,
        update: DashUpdateInfo,
        onProgress: (Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.cacheDir, "updates")
            require(directory.isDirectory || directory.mkdirs()) {
                "Update-Verzeichnis konnte nicht erstellt werden"
            }

            val filename = "DriveChronik-Dash-v${update.version}.apk"
            require(update.apkName == filename) {
                "Unerwarteter APK-Dateiname"
            }

            val prefix =
                "https://github.com/Pischleuder1/drivechronik-dash/releases/download/v${update.version}/"

            require(update.apkUrl == prefix + filename) {
                "Ungültige APK-Adresse"
            }

            val apk = File(directory, filename)
            val temporary = File(directory, "$filename.download")

            temporary.delete()
            apk.delete()

            try {
                val checksums = downloadText(
                    prefix + "SHA256SUMS.txt"
                )

                val expected = checksums.lineSequence()
                    .map { it.trim() }
                    .mapNotNull { line ->
                        val match = Regex(
                            """^([a-fA-F0-9]{64})  \*?(.+)$"""
                        ).matchEntire(line)

                        if (match?.groupValues?.get(2) == filename) {
                            match.groupValues[1].lowercase(Locale.ROOT)
                        } else {
                            null
                        }
                    }
                    .singleOrNull()
                    ?: error("APK-Prüfsumme fehlt oder ist mehrdeutig")

                val connection =
                    URL(update.apkUrl).openConnection()
                        as HttpURLConnection

                try {
                    connection.connectTimeout = 15_000
                    connection.readTimeout = 30_000
                    connection.instanceFollowRedirects = true
                    connection.setRequestProperty(
                        "User-Agent",
                        "DriveChronik-Dash"
                    )

                    require(connection.responseCode == 200) {
                        "APK-Download HTTP ${connection.responseCode}"
                    }

                    val total = connection.contentLengthLong

                    require(total <= MAX_APK_BYTES) {
                        "APK überschreitet die maximale Downloadgröße"
                    }

                    val digest = MessageDigest.getInstance("SHA-256")
                    var downloaded = 0L

                    connection.inputStream.use { input ->
                        temporary.outputStream().use { output ->
                            val buffer = ByteArray(64 * 1024)

                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break

                                output.write(buffer, 0, count)
                                digest.update(buffer, 0, count)
                                downloaded += count

                                require(downloaded <= MAX_APK_BYTES) {
                                    "APK überschreitet die maximale Downloadgröße"
                                }

                                if (total > 0) {
                                    onProgress(
                                        ((downloaded * 100L) / total)
                                            .toInt()
                                            .coerceIn(0, 100)
                                    )
                                }
                            }
                        }
                    }

                    require(downloaded > 0L) {
                        "Leere APK heruntergeladen"
                    }

                    if (total > 0L) {
                        require(downloaded == total) {
                            "APK unvollständig"
                        }
                    }

                    val actual = digest.digest()
                        .joinToString("") {
                            "%02x".format(it.toInt() and 0xff)
                        }

                    require(actual == expected) {
                        "SHA-256-Prüfung fehlgeschlagen"
                    }

                    require(temporary.renameTo(apk)) {
                        "APK konnte nicht bereitgestellt werden"
                    }

                    onProgress(100)
                    apk
                } finally {
                    connection.disconnect()
                }
            } catch (e: Exception) {
                temporary.delete()
                apk.delete()
                throw e
            }
        }
    }

    private fun downloadText(url: String): String {
        val connection =
            URL(url).openConnection() as HttpURLConnection

        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty(
                "User-Agent",
                "DriveChronik-Dash"
            )

            require(connection.responseCode == 200) {
                "Prüfsummen-Download HTTP ${connection.responseCode}"
            }

            return connection.inputStream.bufferedReader()
                .use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
