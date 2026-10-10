package de.drivechronik.dash

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object TrafficDisplaySettings {

    private const val PREFS =
        "navigation_traffic_display"

    private const val KEY_AUTO_POPUPS =
        "auto_popups"

    private const val KEY_SHOW_MARKERS =
        "show_markers"

    private var loaded = false

    var autoPopupsEnabled by mutableStateOf(true)
        private set

    var showMarkers by mutableStateOf(true)
        private set

    fun ensureLoaded(context: Context) {
        if (loaded) return

        val preferences =
            context.applicationContext
                .getSharedPreferences(
                    PREFS,
                    Context.MODE_PRIVATE
                )

        autoPopupsEnabled =
            preferences.getBoolean(
                KEY_AUTO_POPUPS,
                true
            )

        showMarkers =
            preferences.getBoolean(
                KEY_SHOW_MARKERS,
                true
            )

        loaded = true
    }

    fun setAutoPopups(
        context: Context,
        enabled: Boolean
    ) {
        ensureLoaded(context)

        autoPopupsEnabled = enabled

        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_AUTO_POPUPS,
                enabled
            )
            .apply()
    }

    fun setShowMarkers(
        context: Context,
        enabled: Boolean
    ) {
        ensureLoaded(context)

        showMarkers = enabled

        context.applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )
            .edit()
            .putBoolean(
                KEY_SHOW_MARKERS,
                enabled
            )
            .apply()
    }
}
