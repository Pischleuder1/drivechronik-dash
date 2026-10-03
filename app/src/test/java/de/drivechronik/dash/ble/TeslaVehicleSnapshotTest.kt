package de.drivechronik.dash.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class TeslaVehicleSnapshotTest {

    @Test
    fun startsEmpty() {

        val store =
            TeslaVehicleSnapshotStore()

        val snapshot =
            store.current()

        assertNull(
            snapshot.drive
        )

        assertNull(
            snapshot.charge
        )

        assertNull(
            snapshot.location
        )
    }


    @Test
    fun mergesIndependentVehicleStates() {

        val store =
            TeslaVehicleSnapshotStore()

        val drive =
            driveData()

        val charge =
            chargeData()

        val location =
            locationData()


        store.updateDrive(
            data = drive,
            receivedAtNanos = 100L
        )


        var snapshot =
            store.current()


        assertEquals(
            drive,
            snapshot.drive
        )

        assertEquals(
            100L,
            snapshot.driveUpdatedAtNanos
        )

        assertNull(
            snapshot.charge
        )

        assertNull(
            snapshot.location
        )


        store.updateCharge(
            data = charge,
            receivedAtNanos = 200L
        )


        snapshot =
            store.current()


        /*
         * DriveState muss erhalten bleiben.
         */
        assertEquals(
            drive,
            snapshot.drive
        )

        assertEquals(
            charge,
            snapshot.charge
        )

        assertEquals(
            100L,
            snapshot.driveUpdatedAtNanos
        )

        assertEquals(
            200L,
            snapshot.chargeUpdatedAtNanos
        )


        store.updateLocation(
            data = location,
            receivedAtNanos = 300L
        )


        snapshot =
            store.current()


        assertEquals(
            drive,
            snapshot.drive
        )

        assertEquals(
            charge,
            snapshot.charge
        )

        assertEquals(
            location,
            snapshot.location
        )

        assertEquals(
            300L,
            snapshot.locationUpdatedAtNanos
        )
    }


    @Test
    fun replacingDriveStatePreservesOtherSections() {

        val store =
            TeslaVehicleSnapshotStore()


        val firstDrive =
            driveData()


        val secondDrive =
            firstDrive.copy(
                speedFloat = 83.5f,
                power = 51
            )


        val charge =
            chargeData()


        val location =
            locationData()


        store.updateDrive(
            firstDrive,
            100L
        )

        store.updateCharge(
            charge,
            200L
        )

        store.updateLocation(
            location,
            300L
        )


        val snapshot =
            store.updateDrive(
                secondDrive,
                400L
            )


        assertEquals(
            secondDrive,
            snapshot.drive
        )

        assertEquals(
            charge,
            snapshot.charge
        )

        assertEquals(
            location,
            snapshot.location
        )

        assertEquals(
            400L,
            snapshot.driveUpdatedAtNanos
        )

        assertEquals(
            200L,
            snapshot.chargeUpdatedAtNanos
        )

        assertEquals(
            300L,
            snapshot.locationUpdatedAtNanos
        )
    }


    @Test
    fun clearRemovesAllVehicleData() {

        val store =
            TeslaVehicleSnapshotStore()


        store.updateDrive(
            driveData(),
            100L
        )

        store.updateCharge(
            chargeData(),
            200L
        )

        store.updateLocation(
            locationData(),
            300L
        )


        val snapshot =
            store.clear()


        assertNull(
            snapshot.drive
        )

        assertNull(
            snapshot.charge
        )

        assertNull(
            snapshot.location
        )

        assertNull(
            snapshot.driveUpdatedAtNanos
        )

        assertNull(
            snapshot.chargeUpdatedAtNanos
        )

        assertNull(
            snapshot.locationUpdatedAtNanos
        )
    }


    private fun driveData():
        TeslaDriveStateData {

        return TeslaDriveStateData(
            speedFloat = 62.5f,
            speed = 62L,
            power = 44,
            shiftState = "D",
            odometerHundredthsOfMile =
                123456
        )
    }


    private fun chargeData():
        TeslaChargeStateData {

        return TeslaChargeStateData(
            batteryLevel = 73,
            usableBatteryLevel = 71,
            batteryRange = 191.25f,
            estimatedBatteryRange = 176.5f,
            idealBatteryRange = 205.75f,
            chargerPower = 11,
            chargingState = "CHARGING"
        )
    }


    private fun locationData():
        TeslaLocationStateData {

        return TeslaLocationStateData(
            latitude = 52.3001f,
            longitude = 8.6001f,
            headingDegrees = 275L,
            gpsAsOf = 1_800_000_000L,
            nativeLocationSupported = true,
            nativeLatitude = 52.3002f,
            nativeLongitude = 8.6002f,
            nativeCoordinateType = "WGS",
            correctedLatitude = null,
            correctedLongitude = null,
            geoLatitude = 52.3003f,
            geoLongitude = 8.6003f,
            geoHeading = 274.5f,
            geoElevation = 91.2f,
            geoAccuracy = 2.5f,
            estimatedGpsValid = true,
            estimatedToRawDistanceMeters =
                3.7f,
            locationName =
                "Test location"
        )
    }
}
