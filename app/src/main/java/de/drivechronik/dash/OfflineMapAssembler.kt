package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest

internal object OfflineMapAssembler {

    private fun hash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }
    }

    internal var freeSpace: (File) -> Long = {
        android.os.StatFs(it.path).availableBytes
    }

    private val assembleMutex = Mutex()

    suspend fun assemble(
        context: Context,
        pkg: RemoteMapPackage
    ): File = assembleMutex.withLock {
        withContext(Dispatchers.IO) {

        require(pkg.id.matches(Regex("[A-Za-z0-9_-]+")))
        require(pkg.fileName.matches(Regex("[A-Za-z0-9._-]+")))
        require(pkg.fileName != "." && pkg.fileName != "..")
        require(pkg.sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(pkg.parts.isNotEmpty())

        val base = context.getExternalFilesDir(null)
            ?: error("Speicher nicht verfügbar")

        val dir = File(
            base,
            "map-downloads/${pkg.id}/${pkg.sha256.lowercase()}"
        )

        val verified = File(
            dir, pkg.fileName + ".verified"
        )

        if (verified.isFile &&
            verified.length() == pkg.sizeBytes &&
            hash(verified).equals(pkg.sha256, true)) {
            return@withContext verified
        }

        val parts = pkg.parts.map { part ->
            val file = File(dir, part.name)
            check(file.isFile) {
                "Dateiteil fehlt: ${part.name}"
            }
            check(file.length() == part.sizeBytes) {
                "Dateigröße falsch: ${part.name}"
            }
            check(hash(file).equals(part.sha256, true)) {
                "Prüfsumme falsch: ${part.name}"
            }
            file
        }

        check(parts.sumOf { it.length() } == pkg.sizeBytes) {
            "Gesamtgröße stimmt nicht"
        }

        val output = File(dir, pkg.fileName + ".assembling")
        val reserve = 256L * 1024 * 1024

        if (output.exists()) {
            check(output.isFile && output.delete()) {
                "Alte Montage kann nicht entfernt werden"
            }
        }

        check(freeSpace(base) >=
            pkg.sizeBytes + reserve) {
            "Nicht genügend freier Speicher"
        }


        try {
            check(output.createNewFile()) {
                "Temporäre Datei nicht erstellbar"
            }

            java.io.FileOutputStream(output).use { target ->
                parts.forEach { part ->
                    part.inputStream().buffered().use { input ->
                        input.copyTo(target)
                    }
                }
                target.fd.sync()
            }

            check(output.length() == pkg.sizeBytes) {
                "Zusammengesetzte Größe falsch"
            }

            check(hash(output).equals(pkg.sha256, true)) {
                "Gesamtprüfsumme stimmt nicht"
            }

            if (verified.exists()) {
                check(verified.isFile && verified.delete()) {
                    "Beschädigte Ergebnisdatei nicht entfernbar"
                }
            }
            check(!verified.exists()) {
                "Verifizierte Datei bereits vorhanden"
            }

            check(output.renameTo(verified)) {
                "Fertiges Paket konnte nicht gespeichert werden"
            }

            verified
        } catch (e: Exception) {
            output.delete()
            throw e
        }
    }
    }
}
