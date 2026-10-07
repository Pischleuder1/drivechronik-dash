package de.drivechronik.dash

import android.content.Context
import java.io.File

internal data class OfflineMapPackage(
    val id: String,
    val name: String,
    val mapFile: File?,
    val routingFile: File?,
    val available: Boolean
) {
    val mapInstalled: Boolean
        get() = mapFile?.isFile == true

    val routingInstalled: Boolean
        get() = routingFile?.isFile == true
}

internal object OfflineMapRepository {

    fun packages(context: Context): List<OfflineMapPackage> {
        val maps = context.getExternalFilesDir(null)
        val routing = context.filesDir

        return listOf(
            OfflineMapPackage(
                id = "de",
                name = "Deutschland",
                mapFile = maps?.resolve("germany.pmtiles"),
                routingFile = File(
                    routing, "routing/valhalla_tiles.tar"
                ),
                available = true
            ),
            OfflineMapPackage(
                id = "dk",
                name = "Dänemark inkl. Bornholm",
                mapFile = maps?.resolve("denmark.pmtiles"),
                routingFile = null,
                available = false
            ),
            OfflineMapPackage(
                id = "benelux",
                name = "Benelux",
                mapFile = maps?.resolve("benelux.pmtiles"),
                routingFile = null,
                available = false
            )
        )
    }
}
