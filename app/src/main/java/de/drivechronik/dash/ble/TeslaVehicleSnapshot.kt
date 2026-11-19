package de.drivechronik.dash.ble


data class TeslaVehicleSnapshot(

    val drive:
        TeslaDriveStateData? = null,

    val charge:
        TeslaChargeStateData? = null,

    val location:
        TeslaLocationStateData? = null,

    val driveUpdatedAtNanos:
        Long? = null,

    val chargeUpdatedAtNanos:
        Long? = null,

    val locationUpdatedAtNanos:
        Long? = null

)


class TeslaVehicleSnapshotStore(

    initial:
        TeslaVehicleSnapshot =
            TeslaVehicleSnapshot()

) {

    private var snapshot =
        initial


    @Synchronized
    fun current():
        TeslaVehicleSnapshot {

        return snapshot
    }


    @Synchronized
    fun updateDrive(
        data: TeslaDriveStateData,
        receivedAtNanos: Long =
            System.nanoTime()
    ): TeslaVehicleSnapshot {

        snapshot =
            snapshot.copy(
                drive = data,
                driveUpdatedAtNanos =
                    receivedAtNanos
            )

        return snapshot
    }


    @Synchronized
    fun updateCharge(
        data: TeslaChargeStateData,
        receivedAtNanos: Long =
            System.nanoTime()
    ): TeslaVehicleSnapshot {

        snapshot =
            snapshot.copy(
                charge = data,
                chargeUpdatedAtNanos =
                    receivedAtNanos
            )

        return snapshot
    }


    @Synchronized
    fun updateLocation(
        data: TeslaLocationStateData,
        receivedAtNanos: Long =
            System.nanoTime()
    ): TeslaVehicleSnapshot {

        // GPS_VALIDITY_GUARD_763
        // Leere LocationStates aus DriveState-Antworten
        // dürfen die letzte gültige GPS-Position und
        // deren ursprünglichen Zeitstempel nicht ersetzen.
        val lat = data.latitude
        val lon = data.longitude

        val validCoordinates =
            lat != null &&
                lon != null &&
                lat.isFinite() &&
                lon.isFinite() &&
                lat in -90f..90f &&
                lon in -180f..180f &&
                !(lat == 0f && lon == 0f)

        if (!validCoordinates) {
            return snapshot
        }

        snapshot =
            snapshot.copy(
                location = data,
                locationUpdatedAtNanos =
                    receivedAtNanos
            )

        return snapshot
    }


    @Synchronized
    fun clear():
        TeslaVehicleSnapshot {

        snapshot =
            TeslaVehicleSnapshot()

        return snapshot
    }
}
