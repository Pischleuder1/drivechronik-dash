package de.drivechronik.dash.ble

import android.content.Context
import java.util.Locale


data class TeslaLiveProtocolConfig(

    val enabled: Boolean = false,

    val keyEnrollmentConfirmed: Boolean = false,

    val vin: String = ""

) {

    val normalizedVin: String
        get() =
            vin
                .trim()
                .uppercase(
                    Locale.ROOT
                )


    val canStartSession: Boolean
        get() =
            enabled &&
                keyEnrollmentConfirmed &&
                normalizedVin.length == 17
}


class TeslaLiveProtocolSettings(
    context: Context
) {

    companion object {

        private const val PREFS =
            "tesla_live_protocol"

        private const val KEY_ENABLED =
            "enabled"

        private const val KEY_ENROLLMENT_CONFIRMED =
            "key_enrollment_confirmed"

        private const val KEY_VIN =
            "vin"
    }


    private val preferences =
        context
            .applicationContext
            .getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE
            )


    fun load():
        TeslaLiveProtocolConfig {

        return TeslaLiveProtocolConfig(

            enabled =
                preferences.getBoolean(
                    KEY_ENABLED,
                    false
                ),

            keyEnrollmentConfirmed =
                preferences.getBoolean(
                    KEY_ENROLLMENT_CONFIRMED,
                    false
                ),

            vin =
                preferences
                    .getString(
                        KEY_VIN,
                        ""
                    )
                    .orEmpty()
        )
    }


    fun save(
        config:
            TeslaLiveProtocolConfig
    ) {

        preferences
            .edit()
            .putBoolean(
                KEY_ENABLED,
                config.enabled
            )
            .putBoolean(
                KEY_ENROLLMENT_CONFIRMED,
                config
                    .keyEnrollmentConfirmed
            )
            .putString(
                KEY_VIN,
                config.normalizedVin
            )
            .apply()
    }


    fun disable() {

        preferences
            .edit()
            .putBoolean(
                KEY_ENABLED,
                false
            )
            .apply()
    }
}
