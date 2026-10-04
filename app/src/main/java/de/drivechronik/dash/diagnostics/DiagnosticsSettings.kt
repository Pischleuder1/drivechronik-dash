package de.drivechronik.dash.diagnostics

import android.content.Context


class DiagnosticsSettings(
    context: Context
) {

    companion object {

        private const val PREFS =
            "drivechronik_diagnostics"

        private const val KEY_ENABLED =
            "enabled"
    }


    private val preferences =
        context
            .applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )


    fun load():
        Boolean {

        return preferences
            .getBoolean(
                KEY_ENABLED,
                false
            )
    }


    fun save(
        enabled: Boolean
    ) {

        preferences
            .edit()
            .putBoolean(
                KEY_ENABLED,
                enabled
            )
            .apply()
    }
}
