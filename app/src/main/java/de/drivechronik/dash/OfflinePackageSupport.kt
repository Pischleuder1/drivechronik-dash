package de.drivechronik.dash

import android.content.Context
import java.io.File

internal object OfflinePackageSupport {

    private val supportedKinds =
        setOf("map", "routing", "charging")

    /**
     * Kompatibilität für den derzeit aktiven Deutschland-Bestand.
     *
     * Diese Liste ist NICHT mehr die Definition aller erlaubten Pakete.
     * Neue Länder werden ausschließlich über das Schema
     *
     *     <country>-<kind>
     *
     * erkannt.
     */
    val bootstrapIds = listOf(
        "de-map",
        "de-routing",
        "de-charging"
    )

    data class PackageId(
        val country: String,
        val kind: String
    )

    fun parseId(id: String): PackageId? {
        val split = id.lastIndexOf('-')

        if (split <= 0 || split >= id.lastIndex) {
            return null
        }

        val country = id.substring(0, split)
        val kind = id.substring(split + 1)

        if (
            !country.matches(
                Regex("[a-z0-9][a-z0-9-]*")
            )
        ) {
            return null
        }

        if (kind !in supportedKinds) {
            return null
        }

        return PackageId(
            country = country,
            kind = kind
        )
    }

    fun isSupported(id: String): Boolean =
        parseId(id) != null

    /**
     * Offline-Pakete, die beim App-Start tatsächlich relevant sind.
     *
     * - Deutschland bleibt als Bootstrap/Fallback enthalten.
     * - Weitere Länder werden automatisch aus dem gecachten
     *   Katalog aufgenommen, sobald sie installiert, bekannt
     *   oder als Pending-Installation vorgemerkt sind.
     */
    fun startupIds(context: Context): List<String> {
        val result = linkedSetOf<String>()
        // Deutschland nur prüfen, wenn ein Paket tatsächlich
        // vorhanden oder als Installation bekannt ist.
        // Fehlende Dateien mit gespeicherter Version/Prüfsumme
        // bleiben relevant und werden weiterhin als Fehler erkannt.
        bootstrapIds.forEach { id ->
            val relevant =
                runCatching {
                    target(context, id).isFile
                }.getOrDefault(false) ||
                OfflineMapVersions.version(context, id) != null ||
                OfflineMapVersions.hash(context, id) != null ||
                OfflineMapPending.pending(context, id) != null

            if (relevant) {
                result += id
            }
        }

        val cached =
            GitHubMapCatalog.readCached(context)
                .getOrNull()
                .orEmpty()

        cached
            .filter { isSupported(it.id) }
            .forEach { pkg ->
                val relevant =
                    runCatching {
                        target(context, pkg).isFile
                    }.getOrDefault(false) ||
                    OfflineMapVersions.version(
                        context,
                        pkg.id
                    ) != null ||
                    OfflineMapVersions.hash(
                        context,
                        pkg.id
                    ) != null ||
                    OfflineMapPending.pending(
                        context,
                        pkg.id
                    ) != null

                if (relevant) {
                    result += pkg.id
                }
            }

        return result.toList()
    }

    fun isBootstrap(id: String): Boolean =
        id in bootstrapIds

    fun validate(pkg: RemoteMapPackage) {
        val parsed = requireNotNull(
            parseId(pkg.id)
        ) {
            "Ungültige Offline-Paket-ID: ${pkg.id}"
        }

        require(pkg.country == parsed.country) {
            "Paket-ID und country stimmen nicht überein: " +
                "${pkg.id} / ${pkg.country}"
        }

        require(pkg.kind == parsed.kind) {
            "Paket-ID und kind stimmen nicht überein: " +
                "${pkg.id} / ${pkg.kind}"
        }

        require(
            pkg.fileName.matches(
                Regex("[A-Za-z0-9._-]+")
            )
        ) {
            "Ungültiger Dateiname: ${pkg.fileName}"
        }

        require(
            pkg.fileName != "." &&
                pkg.fileName != ".."
        )
    }

    /**
     * Ziel anhand des vollständigen Katalogpakets.
     *
     * Deutschland bleibt vorerst an seinen bisherigen Orten,
     * damit bestehende Installationen ohne Migration weiterlaufen.
     *
     * Neue Länder liegen paketweise getrennt und können dadurch
     * identische Dateinamen wie "valhalla_tiles.tar" verwenden.
     */
    fun target(
        context: Context,
        pkg: RemoteMapPackage
    ): File {
        validate(pkg)

        if (pkg.country == "de") {
            val catalogTarget = legacyGermanTarget(
                context = context,
                kind = pkg.kind,
                fileName = pkg.fileName
            )

            if (catalogTarget.isFile) {
                return catalogTarget
            }

            val legacyTarget = legacyGermanTarget(
                context = context,
                kind = pkg.kind,
                fileName = null
            )

            if (legacyTarget.isFile) {
                android.util.Log.i(
                    "OfflineMaps",
                    "Deutschland Legacy-Fallback: " +
                        legacyTarget.absolutePath
                )
                return legacyTarget
            }

            // Noch nichts installiert:
            // aktueller Katalogpfad bleibt das Downloadziel.
            return catalogTarget
        }

        return when (pkg.kind) {
            "map",
            "charging" -> {
                val root =
                    context.getExternalFilesDir(null)
                        ?: error(
                            "Kartenspeicher fehlt"
                        )

                File(
                    root,
                    "offline/${pkg.country}/${pkg.fileName}"
                )
            }

            "routing" ->
                File(
                    context.filesDir,
                    "routing/${pkg.country}/${pkg.fileName}"
                )

            else ->
                error(
                    "Unbekannter Pakettyp: ${pkg.kind}"
                )
        }
    }

    /**
     * Übergangsfunktion für Code, der momentan nur eine Paket-ID
     * besitzt. Das Paket wird zuerst aus dem gecachten Katalog
     * aufgelöst.
     *
     * Für den bestehenden Deutschlandbestand gibt es zusätzlich
     * einen Legacy-Fallback.
     */
    fun target(
        context: Context,
        id: String
    ): File {
        val parsed = requireNotNull(
            parseId(id)
        ) {
            "Ungültige Offline-Paket-ID: $id"
        }

        GitHubMapCatalog.readCached(context)
            .getOrNull()
            ?.firstOrNull { it.id == id }
            ?.let {
                return target(context, it)
            }

        if (parsed.country == "de") {
            return legacyGermanTarget(
                context = context,
                kind = parsed.kind,
                fileName = null
            )
        }

        error(
            "Kein Katalogeintrag für Offline-Paket $id"
        )
    }

    private fun legacyGermanTarget(
        context: Context,
        kind: String,
        fileName: String?
    ): File =
        when (kind) {
            "map" ->
                File(
                    context.getExternalFilesDir(null)
                        ?: error(
                            "Kartenspeicher fehlt"
                        ),
                    fileName ?: "germany.pmtiles"
                )

            "routing" ->
                File(
                    context.filesDir,
                    "routing/" +
                        (fileName ?: "valhalla_tiles.tar")
                )

            "charging" ->
                File(
                    context.getExternalFilesDir(null)
                        ?: error(
                            "Kartenspeicher fehlt"
                        ),
                    fileName
                        ?: "charging-stations.pmtiles"
                )

            else ->
                error(
                    "Unbekannter Pakettyp: $kind"
                )
        }
}
