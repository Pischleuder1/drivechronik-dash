package de.drivechronik.dash

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object OfflinePendingInstaller {

    suspend fun process(
        context: Context,
        id: String
    ): Boolean = withContext(Dispatchers.IO) {
        require(id == "de-map" || id == "de-routing")

        // Ohne Vormerkung nichts verändern.
        if (OfflineMapPending.pending(context, id) == null) {
            return@withContext false
        }

        // Paket ausschließlich aus dem lokalen Katalog.
        val (pkg, verified) =
            OfflinePendingResolver.resolve(context, id)
                ?: return@withContext false

        val target = OfflineMapInstaller.target(context, pkg)

        // Zuerst eine eventuell unterbrochene
        // Installation wiederherstellen.
        OfflineMapInstaller.recover(context, target, id)

        // Die tatsächliche Datei prüfen.
        val alreadyInstalled =
            target.isFile &&
            target.length() == pkg.sizeBytes &&
            OfflineMapInstaller.sha256(target)
                .equals(pkg.sha256, true)

        if (alreadyInstalled) {
            OfflineMapVersions.saveVerified(
                context, id, pkg.version, pkg.sha256
            )
        } else {
            OfflineMapInstaller.install(
                context, pkg, verified
            )
        }

        // Nur nach erfolgreicher Prüfung/Installation
        // die passende Vormerkung entfernen.
        check(OfflineMapPending.clearIfMatching(
            context, id, pkg.sha256
        )) {
            "Vormerkung konnte nicht abgeschlossen werden"
        }

        // Erst jetzt sind aktive Datei, Versionsstand und Pending
        // vollständig abgeschlossen. Nur den zugehörigen Download-
        // und Assembly-Cache entfernen. Backup/Rollback liegen
        // außerhalb dieses Verzeichnisses und bleiben unangetastet.
        runCatching {
            OfflineMapDownloadCleanup.clean(
                context, pkg.id, pkg.sha256
            )
        }.onFailure {
            android.util.Log.w(
                "OfflineMaps",
                "Download-Cache konnte nach Installation " +
                    "nicht bereinigt werden",
                it
            )
        }

        true
    }
}
