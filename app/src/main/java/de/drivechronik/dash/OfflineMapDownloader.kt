package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

internal object OfflineMapDownloader {

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }

    internal var freeSpace: (File) -> Long = {
        android.os.StatFs(it.absolutePath).availableBytes
    }

    internal var openConnection: (String) -> HttpURLConnection =
        { url ->
            URL(url).openConnection() as HttpURLConnection
        }

    suspend fun downloadPart(
        context: Context,
        pkg: RemoteMapPackage,
        part: RemoteMapPart,
        onProgress: (Long, Long) -> Unit
    ): File = withContext(Dispatchers.IO) {

        require(
            part.name.matches(Regex("[A-Za-z0-9._-]+"))
        )
        require(part.name != "." && part.name != "..")
        require(part.sizeBytes > 0L)
        require(
            part.sha256.matches(Regex("[a-fA-F0-9]{64}"))
        )
        require(
            part.url.startsWith(
                "https://github.com/Pischleuder1/" +
                "drivechronik-maps/releases/download/"
            )
        )

        require(pkg.id.matches(Regex("[A-Za-z0-9_-]+")))
        require(pkg.sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(pkg.parts.any {
            it.name == part.name &&
            it.sha256.equals(part.sha256, true) &&
            it.sizeBytes == part.sizeBytes &&
            it.url == part.url
        })

        val base = context.getExternalFilesDir(null)
            ?: error("Speicher nicht verfügbar")

        val dir = File(
            base,
            "map-downloads/${pkg.id}/${pkg.sha256.lowercase()}"
        )
        check(dir.isDirectory || dir.mkdirs())

        val file = File(dir, part.name)
        val partial = File(dir, part.name + ".download")

        if (file.isFile) {
            if (file.length() == part.sizeBytes &&
                sha256(file).equals(part.sha256, true)) {
                onProgress(part.sizeBytes, part.sizeBytes)
                return@withContext file
            }
            check(file.delete()) {
                "Beschädigten Cache nicht entfernbar"
            }
        }

        var offset = if (partial.isFile) {
            partial.length()
        } else {
            0L
        }

        check(offset <= part.sizeBytes) {
            "Unplausible Dateigröße"
        }

        if (offset == part.sizeBytes) {
            if (sha256(partial).equals(part.sha256, true)) {
                check(partial.renameTo(file))
                onProgress(part.sizeBytes, part.sizeBytes)
                return@withContext file
            }
            check(partial.delete()) {
                "Beschädigten Teil nicht entfernbar"
            }
            offset = 0L
        }

        val free = freeSpace(dir)

        val needed = part.sizeBytes - offset
        val reserve = 256L * 1024 * 1024

        check(free >= needed + reserve) {
            "Zu wenig freier Speicher"
        }

        val connection =
            openConnection(part.url)

        try {
            connection.connectTimeout = 15000
            connection.readTimeout = 30000

            if (offset > 0L) {
                connection.setRequestProperty(
                    "Range", "bytes=$offset-"
                )
            }

            val code = connection.responseCode
            if (offset > 0L) {
                check(code == 206) {
                    "Fortsetzung nicht möglich: HTTP $code"
                }
                val range = connection.getHeaderField(
                    "Content-Range"
                )
                check(range ==
                    "bytes $offset-${part.sizeBytes - 1}/${part.sizeBytes}"
                ) {
                    "Ungültiger Content-Range: $range"
                }
            } else {
                check(code == 200) {
                    "Downloadfehler: HTTP $code"
                }
            }

            val remaining = part.sizeBytes - offset
            check(connection.contentLengthLong == remaining) {
                "Unerwartete Downloadgröße"
            }

            var received = offset
            onProgress(received, part.sizeBytes)


            connection.inputStream.use { input ->
                java.io.FileOutputStream(partial, true).use { output ->
                    val buffer = ByteArray(256 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        check(count <= part.sizeBytes - received) {
                            "Server sendet zu viele Daten"
                        }
                        output.write(buffer, 0, count)
                        received += count
                        onProgress(received, part.sizeBytes)
                    }
                }
            }

        } finally {
            connection.disconnect()
        }

        check(partial.length() == part.sizeBytes) {
            "Download unvollständig"
        }
        check(sha256(partial).equals(
            part.sha256, ignoreCase = true
        )) {
            "Prüfsumme stimmt nicht"
        }
        check(partial.renameTo(file)) {
            "Speichern fehlgeschlagen"
        }
        file
    }
}
