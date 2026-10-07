package de.drivechronik.dash

import android.content.Context
import android.util.Log
import java.io.File

internal object OfflineMapDownloadCleanup {

    fun clean(
        context: Context,
        packageId: String,
        sha256: String
    ): Boolean {
        require(packageId.matches(Regex("[A-Za-z0-9._-]+"))) {
            "Ungültige Paket-ID"
        }
        require(sha256.matches(Regex("[0-9a-fA-F]{64}"))) {
            "Ungültige SHA-256-Prüfsumme"
        }

        val root = context.getExternalFilesDir(null)
            ?.resolve("map-downloads")
            ?: return true

        val packageDir = File(root, packageId)
        val versionDir = File(packageDir, sha256.lowercase())

        if (versionDir.exists() && !versionDir.deleteRecursively()) {
            Log.w(
                "OfflineMaps",
                "Download-Cache konnte nicht vollständig entfernt werden: " +
                    versionDir.absolutePath
            )
            return false
        }

        // Paketordner nur entfernen, wenn er wirklich leer ist.
        if (packageDir.isDirectory &&
            packageDir.listFiles()?.isEmpty() == true
        ) {
            packageDir.delete()
        }

        return !versionDir.exists()
    }
}
