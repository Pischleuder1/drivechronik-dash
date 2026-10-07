package de.drivechronik.dash

import android.content.Context

internal object OfflineMapVersions {

    private fun prefs(context: Context) =
        context.getSharedPreferences(
            "offline_map_versions", Context.MODE_PRIVATE
        )

    fun remember(
        context: Context,
        pkg: RemoteMapPackage
    ) {
        prefs(context).edit()
            .putString("${pkg.id}.version", pkg.version)
            .putString("${pkg.id}.sha256", pkg.sha256)
            .apply()
    }

    fun saveVerified(
        context: Context,
        id: String,
        version: String,
        sha256: String
    ) {
        require(id.matches(Regex("[A-Za-z0-9_-]+")))
        require(sha256.matches(Regex("[a-fA-F0-9]{64}")))

        check(
            prefs(context).edit()
                .putString("$id.version", version)
                .putString("$id.sha256", sha256)
                .commit()
        ) {
            "Versionsdaten konnten nicht gespeichert werden"
        }
    }

    fun version(
        context: Context,
        id: String
    ): String? = prefs(context)
        .getString("$id.version", null)

    fun hash(
        context: Context,
        id: String
    ): String? = prefs(context)
        .getString("$id.sha256", null)
}
