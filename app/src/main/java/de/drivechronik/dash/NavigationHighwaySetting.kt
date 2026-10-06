package de.drivechronik.dash

import android.content.Context

internal object NavigationHighwaySetting {
    private const val KEY = "avoid_highways"

    fun enabled(context: Context): Boolean =
        context.getSharedPreferences(
            "navigation_options", Context.MODE_PRIVATE
        ).getBoolean(KEY, false)

    fun set(context: Context, value: Boolean) {
        context.getSharedPreferences(
            "navigation_options", Context.MODE_PRIVATE
        ).edit().putBoolean(KEY, value).apply()
    }
}
