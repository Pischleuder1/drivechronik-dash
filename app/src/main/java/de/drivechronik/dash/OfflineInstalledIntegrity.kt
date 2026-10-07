package de.drivechronik.dash

import android.content.Context

internal object OfflineInstalledIntegrity {

    fun verify(context: Context) {
        verify(context, "de-map")
        verify(context, "de-routing")
    }

    fun verify(context: Context, id: String) {
        require(id == "de-map" || id == "de-routing")
        val items = listOf(
            "de-map" to context.getExternalFilesDir(null)
                ?.resolve("germany.pmtiles"),
            "de-routing" to context.filesDir
                .resolve("routing/valhalla_tiles.tar")
        )

        for ((packageId, file) in items.filter { it.first == id }) {
            val expected = OfflineMapVersions.hash(context, packageId)

            // Alte, manuell installierte Dateien ohne
            // gespeicherte Prüfsumme nicht verändern.
            if (expected == null) continue

            check(file?.isFile == true) {
                "Installierte Datei fehlt: $packageId"
            }

            check(
                OfflineMapInstaller.sha256(file)
                    .equals(expected, true)
            ) {
                "Integritätsprüfung fehlgeschlagen: $packageId"
            }
        }
    }
}
