package de.drivechronik.dash.ble


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
