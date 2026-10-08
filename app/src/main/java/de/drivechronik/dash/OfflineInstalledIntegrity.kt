package de.drivechronik.dash

import android.content.Context

internal object OfflineInstalledIntegrity {

    fun verify(context: Context) {
        OfflinePackageSupport.startupIds(context).forEach { id ->
            verify(context, id)
        }
    }

    fun verify(
        context: Context,
        id: String
    ) {
        require(OfflinePackageSupport.isSupported(id))

        val file = OfflinePackageSupport.target(context, id)
        val expected = OfflineMapVersions.hash(context, id)

        // Alte, manuell installierte Dateien ohne gespeicherte
        // Prüfsumme nicht verändern.
        if (expected == null) return

        check(file.isFile) {
            "Installierte Datei fehlt: $id"
        }

        check(
            OfflineMapInstaller.sha256(file)
                .equals(expected, true)
        ) {
            "Integritätsprüfung fehlgeschlagen: $id"
        }
    }
}
