package de.drivechronik.dash

import android.content.Context

/**
 * Persistente Auswahl des Dashboard-Layouts.
 *
 * CLASSIC ist immer der sichere Standard.
 */
internal class DashboardLayoutSettings(
    context: Context
) {

    private val preferences =
        context.applicationContext.getSharedPreferences(
            "dashboard_layout_settings",
            Context.MODE_PRIVATE
        )

    fun load(): DashboardLayoutMode {
        val saved = preferences.getString(
            "layout_mode",
            DashboardLayoutMode.CLASSIC.name
        )

        return DashboardLayoutMode.entries.firstOrNull {
            it.name == saved
        } ?: DashboardLayoutMode.CLASSIC
    }

    fun save(mode: DashboardLayoutMode) {
        preferences.edit()
            .putString("layout_mode", mode.name)
            .apply()
    }
}
