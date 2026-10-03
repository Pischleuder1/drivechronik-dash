package de.drivechronik.dash.ble

import java.util.Locale


data class TeslaDashboardTelemetry(

    /*
     * Noch bewusst ohne Einheit.
     *
     * speedFloat wird bevorzugt,
     * speed dient nur als Fallback.
     */
    val speedRaw: Float? = null,

    /*
     * Einheit wird erst nach
     * Fahrzeug-Verifikation festgelegt.
     */
    val powerRaw: Int? = null,

    val gear: String? = null,

    /*
     * Navigationsdaten stammen direkt aus
     * Teslas aktivem DriveState.
     */
    val activeRouteDestination: String? = null,

    val activeRouteMinutesToArrival: Float? = null,

    val activeRouteMilesToArrival: Float? = null,

    val activeRouteTrafficMinutesDelay: Float? = null,

    val activeRouteEnergyAtArrival: Float? = null,

    val batteryLevel: Int? = null,

    val usableBatteryLevel: Int? = null,

    val batteryRangeRaw: Float? = null,

    val estimatedBatteryRangeRaw: Float? = null,

    val idealBatteryRangeRaw: Float? = null,

    val latitude: Float? = null,

    val longitude: Float? = null,

    val headingDegrees: Long? = null,

    /*
     * Alter der einzelnen Datenblöcke.
     *
     * Damit kann die UI später z. B.
     * Geschwindigkeit schneller als
     * Akku oder Position altern lassen.
     */
    val driveAgeMillis: Long? = null,

    val chargeAgeMillis: Long? = null,

    val locationAgeMillis: Long? = null

)


data class TeslaNavigationTelemetry(

    val destination: String?,

    val remainingDistanceKm: Float?,

    val remainingMinutes: Float?,

    val trafficDelayMinutes: Float?,

    val energyAtArrivalPercent: Float?

)


object TeslaDashboardTelemetryMapper {

    fun map(
        snapshot: TeslaVehicleSnapshot,
        nowNanos: Long =
            System.nanoTime()
    ): TeslaDashboardTelemetry {

        val drive =
            snapshot.drive

        val charge =
            snapshot.charge

        val location =
            snapshot.location


        val speed =
            drive
                ?.speedFloat
                ?: drive
                    ?.speed
                    ?.toFloat()


        return TeslaDashboardTelemetry(

            speedRaw =
                speed,

            powerRaw =
                drive?.power,

            gear =
                drive?.shiftState,

            activeRouteDestination =
                drive?.activeRouteDestination,

            activeRouteMinutesToArrival =
                drive?.activeRouteMinutesToArrival,

            activeRouteMilesToArrival =
                drive?.activeRouteMilesToArrival,

            activeRouteTrafficMinutesDelay =
                drive?.activeRouteTrafficMinutesDelay,

            activeRouteEnergyAtArrival =
                drive?.activeRouteEnergyAtArrival,

            batteryLevel =
                charge?.batteryLevel,

            usableBatteryLevel =
                charge?.usableBatteryLevel,

            batteryRangeRaw =
                charge?.batteryRange,

            estimatedBatteryRangeRaw =
                charge?.estimatedBatteryRange,

            idealBatteryRangeRaw =
                charge?.idealBatteryRange,

            latitude =
                location?.latitude,

            longitude =
                location?.longitude,

            headingDegrees =
                location?.headingDegrees,

            driveAgeMillis =
                ageMillis(
                    snapshot
                        .driveUpdatedAtNanos,
                    nowNanos
                ),

            chargeAgeMillis =
                ageMillis(
                    snapshot
                        .chargeUpdatedAtNanos,
                    nowNanos
                ),

            locationAgeMillis =
                ageMillis(
                    snapshot
                        .locationUpdatedAtNanos,
                    nowNanos
                )
        )
    }


    /*
     * Drive-Daten sollen im Fahrerdisplay nicht
     * unbegrenzt weiter angezeigt werden.
     */
    fun isDriveFresh(
        telemetry: TeslaDashboardTelemetry,
        maxAgeMillis: Long = 3_000L
    ): Boolean {

        val age =
            telemetry.driveAgeMillis
                ?: return false

        return age <= maxAgeMillis
    }


    /*
     * Tesla liefert shiftState als String.
     *
     * Für das Dashboard werden ausschließlich
     * P / R / N / D dargestellt.
     *
     * Bei veralteten oder unbekannten Daten:
     * null -> UI zeigt "—".
     */
    fun displayGear(
        telemetry: TeslaDashboardTelemetry,
        maxAgeMillis: Long = 3_000L
    ): String? {

        if (
            !isDriveFresh(
                telemetry,
                maxAgeMillis
            )
        ) {
            return null
        }

        val raw =
            telemetry.gear
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?: return null

        return when (raw) {

            "P",
            "PARK",
            "SHIFT_STATE_PARK" ->
                "P"

            "R",
            "REVERSE",
            "SHIFT_STATE_REVERSE" ->
                "R"

            "N",
            "NEUTRAL",
            "SHIFT_STATE_NEUTRAL" ->
                "N"

            "D",
            "DRIVE",
            "SHIFT_STATE_DRIVE" ->
                "D"

            else ->
                null
        }
    }


    fun activeNavigation(
        telemetry: TeslaDashboardTelemetry,
        maxAgeMillis: Long = 3_000L
    ): TeslaNavigationTelemetry? {

        if (
            !isDriveFresh(
                telemetry,
                maxAgeMillis
            )
        ) {
            return null
        }

        val destination =
            telemetry
                .activeRouteDestination
                ?.trim()
                ?.takeIf {
                    it.isNotEmpty()
                }

        val minutes =
            telemetry
                .activeRouteMinutesToArrival
                ?.takeIf {
                    it >= 0f
                }

        val miles =
            telemetry
                .activeRouteMilesToArrival
                ?.takeIf {
                    it >= 0f
                }

        val traffic =
            telemetry
                .activeRouteTrafficMinutesDelay
                ?.takeIf {
                    it >= 0f
                }

        val energy =
            telemetry
                .activeRouteEnergyAtArrival
                ?.takeIf {
                    it in 0f..100f
                }

        /*
         * Ohne irgendeinen activeRoute-Wert
         * existiert für das Dashboard keine
         * aktive Tesla-Navigation.
         */
        if (
            destination == null &&
            minutes == null &&
            miles == null &&
            traffic == null &&
            energy == null
        ) {
            return null
        }

        return TeslaNavigationTelemetry(
            destination = destination,

            remainingDistanceKm =
                miles?.times(
                    1.609344f
                ),

            remainingMinutes =
                minutes,

            trafficDelayMinutes =
                traffic,

            energyAtArrivalPercent =
                energy
        )
    }


    private fun ageMillis(
        updatedAtNanos: Long?,
        nowNanos: Long
    ): Long? {

        if (
            updatedAtNanos == null
        ) {
            return null
        }

        val ageNanos =
            (
                nowNanos -
                    updatedAtNanos
            )
                .coerceAtLeast(
                    0L
                )

        return ageNanos /
            1_000_000L
    }
}
