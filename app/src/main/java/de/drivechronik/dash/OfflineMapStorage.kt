package de.drivechronik.dash

import android.content.Context
import android.os.StatFs
import java.io.File
import java.security.MessageDigest

object OfflineMapStorage {
    private fun valid(file: File, bytes: Long, hash: String): Boolean {
        if (!file.isFile || file.length() != bytes) return false
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                md.update(buffer, 0, n)
            }
        }
        return md.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }.equals(hash, true)
    }

    internal var freeSpace: (File) -> Long = {
        StatFs(it.path).availableBytes
    }

    internal fun check(context: Context, pkg: RemoteMapPackage) {
        val base = context.getExternalFilesDir(null)
            ?: error("Kartenspeicher fehlt")
        val dir = File(
            base, "map-downloads/${pkg.id}/${pkg.sha256.lowercase()}"
        )
        require(pkg.sizeBytes > 0)
        require(pkg.parts.isNotEmpty())
        require(pkg.parts.all { it.sizeBytes > 0 })
        require(pkg.parts.sumOf { it.sizeBytes } == pkg.sizeBytes)

        val ready = valid(
            File(dir, pkg.fileName + ".verified"),
            pkg.sizeBytes, pkg.sha256
        )
        val missing = if (ready) 0L else pkg.parts.sumOf {
            if (valid(File(dir, it.name), it.sizeBytes, it.sha256))
                0L else it.sizeBytes
        }
        // Download und Montage, ohne Installationskopie.
        val reserve = 3L * 1024 * 1024 * 1024
        val needed = Math.addExact(
            Math.addExact(missing, if (ready) 0L else pkg.sizeBytes),
            reserve
        )
        val free = freeSpace(base)
        check(free >= needed) {
            "Zu wenig Speicher: benötigt %.1f GB, frei %.1f GB"
                .format(needed / 1e9, free / 1e9)
        }
    }
}
