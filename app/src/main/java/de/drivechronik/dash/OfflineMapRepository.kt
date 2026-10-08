package de.drivechronik.dash

import android.content.Context
import java.io.File

internal data class OfflineMapPackage(
    val id: String,
    val name: String,
    val mapPackage: RemoteMapPackage?,
    val routingPackage: RemoteMapPackage?,
    val chargingPackage: RemoteMapPackage?,
    val mapFile: File?,
    val routingFile: File?,
    val chargingFile: File?
) {
    val mapInstalled: Boolean
        get() = mapFile?.isFile == true

    val routingInstalled: Boolean
        get() = routingFile?.isFile == true

    val chargingInstalled: Boolean
        get() = chargingFile?.isFile == true
}

internal object OfflineMapRepository {

    private const val PREFS =
        "offline_region_selection"

    private const val KEY_ACTIVE_COUNTRY =
        "active_country"

    /**
     * Aktive Region für Kartendarstellung und Routing.
     *
     * Deutschland bleibt solange Standard, bis der Benutzer
     * später explizit ein anderes Land auswählt.
     */
    fun activeCountry(context: Context): String =
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getString(
            KEY_ACTIVE_COUNTRY,
            "de"
        ) ?: "de"

    fun setActiveCountry(
        context: Context,
        country: String
    ) {
        require(
            country.matches(
                Regex("[a-z0-9][a-z0-9-]*")
            )
        )

        check(
            context.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            ).edit()
                .putString(
                    KEY_ACTIVE_COUNTRY,
                    country
                )
                .commit()
        )
    }

    /**
     * Alle Länder/Regionen, die der aktuelle Katalog kennt.
     *
     * Die Paketstruktur entsteht vollständig aus:
     *
     *   <country>-map
     *   <country>-routing
     *   <country>-charging
     *
     * Es gibt hier keine fest codierte Länder-Liste mehr.
     */
    fun packages(
        context: Context
    ): List<OfflineMapPackage> {

        val remote =
            GitHubMapCatalog.readCached(context)
                .getOrNull()
                .orEmpty()
                .filter {
                    OfflinePackageSupport
                        .isSupported(it.id)
                }

        val countries =
            remote
                .map { it.country }
                .toMutableSet()
                .apply {
                    // Deutschland bleibt Bootstrap/Fallback,
                    // auch wenn noch kein Katalogcache existiert.
                    add("de")
                }
                .sorted()

        return countries.map { country ->
            packageFor(
                context = context,
                country = country,
                remote = remote
            )
        }
    }

    fun activePackage(
        context: Context
    ): OfflineMapPackage =
        packageFor(
            context = context,
            country = activeCountry(context),
            remote =
                GitHubMapCatalog.readCached(context)
                    .getOrNull()
                    .orEmpty()
                    .filter {
                        OfflinePackageSupport
                            .isSupported(it.id)
                    }
        )

    fun packageFor(
        context: Context,
        country: String
    ): OfflineMapPackage =
        packageFor(
            context = context,
            country = country,
            remote =
                GitHubMapCatalog.readCached(context)
                    .getOrNull()
                    .orEmpty()
                    .filter {
                        OfflinePackageSupport
                            .isSupported(it.id)
                    }
        )

    private fun packageFor(
        context: Context,
        country: String,
        remote: List<RemoteMapPackage>
    ): OfflineMapPackage {

        val map =
            remote.firstOrNull {
                it.country == country &&
                    it.kind == "map"
            }

        val routing =
            remote.firstOrNull {
                it.country == country &&
                    it.kind == "routing"
            }

        val charging =
            remote.firstOrNull {
                it.country == country &&
                    it.kind == "charging"
            }

        return OfflineMapPackage(
            id = country,

            name = displayName(
                country = country,
                packages = listOfNotNull(
                    map,
                    routing,
                    charging
                )
            ),

            mapPackage = map,
            routingPackage = routing,
            chargingPackage = charging,

            mapFile =
                targetOrLegacy(
                    context,
                    country,
                    "map",
                    map
                ),

            routingFile =
                targetOrLegacy(
                    context,
                    country,
                    "routing",
                    routing
                ),

            chargingFile =
                targetOrLegacy(
                    context,
                    country,
                    "charging",
                    charging
                )
        )
    }

    private fun targetOrLegacy(
        context: Context,
        country: String,
        kind: String,
        pkg: RemoteMapPackage?
    ): File? {

        if (pkg != null) {
            return runCatching {
                OfflinePackageSupport.target(
                    context,
                    pkg
                )
            }.getOrNull()
        }

        // Deutschland muss auch vor dem ersten
        // erfolgreichen Katalogabruf funktionieren.
        if (country == "de") {
            return runCatching {
                OfflinePackageSupport.target(
                    context,
                    "$country-$kind"
                )
            }.getOrNull()
        }

        return null
    }

    private fun displayName(
        country: String,
        packages: List<RemoteMapPackage>
    ): String {

        packages
            .firstOrNull {
                it.countryName.isNotBlank()
            }
            ?.countryName
            ?.let {
                return it
            }

        return when (country) {
            "de" -> "Deutschland"
            else -> country.uppercase()
        }
    }
}
