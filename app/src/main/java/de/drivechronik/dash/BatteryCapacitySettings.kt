package de.drivechronik.dash

import android.content.Context

/**
 * Lokal hinterlegte nutzbare Akkukapazität.
 *
 * Kein Tesla-BLE-Messwert.
 * Kein voreingestellter Fahrzeugwert.
 * Keine Änderung an Tesla-Schlüsseln oder Kopplung.
 */
internal class BatteryCapacitySettings(
    context: Context
) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            "battery_capacity_settings",
            Context.MODE_PRIVATE
        )

    fun load(): Float? {
        if (!preferences.contains(KEY_CAPACITY)) {
            return null
        }

        val value = preferences.getFloat(
            KEY_CAPACITY,
            Float.NaN
        )

        return value.takeIf(::isValid)
    }

    fun save(capacityKwh: Float): Boolean {
        if (!isValid(capacityKwh)) {
            return false
        }

        preferences.edit()
            .putFloat(KEY_CAPACITY, capacityKwh)
            .apply()

        return true
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_CAPACITY)
            .apply()
    }

    /**
     * Informiert aktive Anzeigen über Änderungen.
     * Gibt eine Funktion zur Abmeldung zurück.
     */
    fun observe(
        onChanged: (Float?) -> Unit
    ): () -> Unit {
        val listener =
            android.content.SharedPreferences
                .OnSharedPreferenceChangeListener { _, key ->
                    if (key == KEY_CAPACITY) {
                        onChanged(load())
                    }
                }

        preferences.registerOnSharedPreferenceChangeListener(
            listener
        )

        onChanged(load())

        return {
            preferences.unregisterOnSharedPreferenceChangeListener(
                listener
            )
        }
    }

    companion object {
        private const val KEY_CAPACITY =
            "usable_capacity_kwh"

        fun isValid(value: Float): Boolean =
            value.isFinite() &&
                value in 20f..150f
    }
}
