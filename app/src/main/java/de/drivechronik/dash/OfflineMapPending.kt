package de.drivechronik.dash

import android.content.Context
import java.io.File

internal object OfflineMapPending {

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "offline_map_pending",
            Context.MODE_PRIVATE
        )

    fun mark(
        context: Context,
        pkg: RemoteMapPackage,
        verified: File
    ) {
        require(pkg.id.matches(Regex("[A-Za-z0-9_-]+")))
        require(pkg.sha256.matches(Regex("[a-fA-F0-9]{64}")))
        require(pkg.fileName.matches(Regex("[A-Za-z0-9._-]+")))
        require(pkg.fileName != "." && pkg.fileName != "..")

        val base = context.getExternalFilesDir(null)
            ?: error("Speicher nicht verfügbar")

        val expected = File(
            base,
            "map-downloads/${pkg.id}/" +
                "${pkg.sha256.lowercase()}/" +
                "${pkg.fileName}.verified"
        )

        check(verified.canonicalFile == expected.canonicalFile)
        check(verified.isFile && verified.length() == pkg.sizeBytes)


        val digest = java.security.MessageDigest
            .getInstance("SHA-256")

        verified.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }

        val actual = digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 255)
        }

        check(actual.equals(pkg.sha256, true)) {
            "Paketprüfung fehlgeschlagen"
        }

        check(prefs(context).edit()
            .putString("${pkg.id}.sha256", pkg.sha256)
            .putString("${pkg.id}.version", pkg.version)
            .commit()) {
            "Installationsvormerkung fehlgeschlagen"
        }
    }

    fun pending(context: Context, id: String): String? =
        prefs(context).getString("$id.sha256", null)

    @Synchronized
    fun clearIfMatching(
        context: Context,
        id: String,
        sha256: String
    ): Boolean {
        val settings = prefs(context)
        val current = settings.getString("$id.sha256", null)
        if (!current.equals(sha256, true)) return false

        return settings.edit()
            .remove("$id.sha256")
            .remove("$id.version")
            .commit()
    }


    fun pendingVersion(context: Context, id: String): String? =
        prefs(context).getString("$id.version", null)

}
