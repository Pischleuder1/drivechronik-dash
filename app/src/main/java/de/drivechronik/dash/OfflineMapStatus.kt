package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

internal enum class MapInstallStatus {
    NOT_INSTALLED,
    CURRENT,
    UPDATE_AVAILABLE,
    UNKNOWN_VERSION
}

internal object OfflineMapStatus {

    private fun installedFile(
        context: Context,
        pkg: RemoteMapPackage
    ): File? {
        if (pkg.country != "de") return null

        return when (pkg.kind) {
            "map" -> context.getExternalFilesDir(null)
                ?.resolve("germany.pmtiles")
            "routing" -> File(
                context.filesDir,
                "routing/valhalla_tiles.tar"
            )
            else -> null
        }
    }

    suspend fun check(
        context: Context,
        pkg: RemoteMapPackage
    ): MapInstallStatus = withContext(Dispatchers.IO) {
        val file = installedFile(context, pkg)

        if (file?.isFile != true) {
            return@withContext MapInstallStatus.NOT_INSTALLED
        }

        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }

        val hash = digest.digest().joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }

        if (hash.equals(pkg.sha256, ignoreCase = true)) {
            OfflineMapVersions.remember(context, pkg)
            MapInstallStatus.CURRENT
        } else {
            val previousHash =
                OfflineMapVersions.hash(context, pkg.id)

            if (
                previousHash != null &&
                hash.equals(previousHash, ignoreCase = true) &&
                !hash.equals(pkg.sha256, ignoreCase = true)
            ) {
                MapInstallStatus.UPDATE_AVAILABLE
            } else {
                MapInstallStatus.UNKNOWN_VERSION
            }
        }
    }
}
