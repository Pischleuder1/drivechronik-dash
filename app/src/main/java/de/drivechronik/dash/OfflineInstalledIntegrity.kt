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

        // Auch bei alten/manuellen Installationen ohne gespeicherte
        // Prüfsumme muss die eigentliche Datei vorhanden sein.
        check(file.isFile) {
            "Installierte Datei fehlt: $id"
        }

        // Ohne bekannte Prüfsumme können wir die Datei nicht gegen
        // einen gespeicherten Hash prüfen, akzeptieren sie aber als
        // vorhandenen Legacy-Bestand.
        if (expected == null) return

        check(
            OfflineMapInstaller.sha256(file)
                .equals(expected, true)
        ) {
            "Integritätsprüfung fehlgeschlagen: $id"
        }
    }
}
