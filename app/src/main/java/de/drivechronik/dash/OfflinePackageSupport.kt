package de.drivechronik.dash

import android.content.Context
import java.io.File

internal object OfflinePackageSupport {

    val supportedIds = listOf(
        "de-map",
        "de-routing",
        "de-charging"
    )

    fun isSupported(id: String): Boolean =
        id in supportedIds

    fun validate(pkg: RemoteMapPackage) {
        require(pkg.id in supportedIds)
        require(pkg.country == "de")

        when (pkg.id) {
            "de-map" -> {
                require(pkg.kind == "map")
                require(pkg.fileName == "germany.pmtiles")
            }

            "de-routing" -> {
                require(pkg.kind == "routing")
                require(pkg.fileName == "valhalla_tiles.tar")
            }

            "de-charging" -> {
                require(pkg.kind == "charging")
                require(pkg.fileName == "charging-stations.pmtiles")
            }
        }
    }

    fun target(
        context: Context,
        id: String
    ): File {
        require(id in supportedIds)

        return when (id) {
            "de-map" ->
                File(
                    context.getExternalFilesDir(null)
                        ?: error("Kartenspeicher fehlt"),
                    "germany.pmtiles"
                )

            "de-routing" ->
                File(
                    context.filesDir,
                    "routing/valhalla_tiles.tar"
                )

            "de-charging" ->
                File(
                    context.getExternalFilesDir(null)
                        ?: error("Kartenspeicher fehlt"),
                    "charging-stations.pmtiles"
                )

            else -> error("Unbekanntes Offline-Paket")
        }
    }

    fun target(
        context: Context,
        pkg: RemoteMapPackage
    ): File {
        validate(pkg)
        return target(context, pkg.id)
    }
}
