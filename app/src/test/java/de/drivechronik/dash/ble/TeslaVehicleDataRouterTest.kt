package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer
import com.tesla.generated.carserver.vehicle.Vehicle
import de.drivechronik.dash.data.TeslaDashboardRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaVehicleDataRouterTest {

    @Test
    fun routesMultipleVehicleSections() {

        val drive =
            Vehicle
                .DriveState
                .newBuilder()
                .setSpeedFloat(
                    82.5f
                )
                .setPower(
                    31
                )
                .build()


        val charge =
            Vehicle
                .ChargeState
                .newBuilder()
                .setBatteryLevel(
                    72
                )
                .setUsableBatteryLevel(
                    70
                )
                .build()


        val location =
            Vehicle
                .LocationState
                .newBuilder()
                .setLatitude(
                    52.3f
                )
                .setLongitude(
                    8.6f
                )
                .setHeading(
                    275
                )
                .build()


        val vehicleData =
            Vehicle
                .VehicleData
                .newBuilder()
                .setDriveState(
                    drive
                )
                .setChargeState(
                    charge
                )
                .setLocationState(
                    location
                )
                .build()


        val response =
            CarServer
                .Response
                .newBuilder()
                .setVehicleData(
                    vehicleData
                )
                .build()


        val update =
            TeslaVehicleDataRouter
                .decode(
                    response.toByteArray()
                )


        assertTrue(
            update.hasVehicleData
        )

        assertNotNull(
            update.drive
        )

        assertNotNull(
            update.charge
        )

        assertNotNull(
            update.location
        )


        assertEquals(
            82.5f,
            update.drive?.speedFloat
        )

        assertEquals(
            31,
            update.drive?.power
        )

        assertEquals(
            72,
            update.charge?.batteryLevel
        )

        assertEquals(
            52.3f,
            update.location?.latitude
        )

        assertEquals(
            275L,
            update.location?.headingDegrees
        )
    }


    @Test
    fun responseWithoutVehicleDataIsIgnored() {

        val response =
            CarServer
                .Response
                .newBuilder()
                .build()


        val update =
            TeslaVehicleDataRouter
                .decode(
                    response.toByteArray()
                )


        assertFalse(
            update.hasVehicleData
        )
    }


    @Test
    fun repositoryMergesRouterUpdates() {

        TeslaDashboardRepository
            .clear()


        val driveResponse =
            CarServer
                .Response
                .newBuilder()
                .setVehicleData(
                    Vehicle
                        .VehicleData
                        .newBuilder()
                        .setDriveState(
                            Vehicle
                                .DriveState
                                .newBuilder()
                                .setSpeedFloat(
                                    66.5f
                                )
                                .setPower(
                                    25
                                )
                        )
                )
                .build()


        val chargeResponse =
            CarServer
                .Response
                .newBuilder()
                .setVehicleData(
                    Vehicle
                        .VehicleData
                        .newBuilder()
                        .setChargeState(
                            Vehicle
                                .ChargeState
                                .newBuilder()
                                .setBatteryLevel(
                                    81
                                )
                        )
                )
                .build()


        TeslaDashboardRepository
            .apply(
                update =
                    TeslaVehicleDataRouter
                        .decode(
                            driveResponse
                                .toByteArray()
                        ),
                receivedAtNanos =
                    100L
            )


        TeslaDashboardRepository
            .apply(
                update =
                    TeslaVehicleDataRouter
                        .decode(
                            chargeResponse
                                .toByteArray()
                        ),
                receivedAtNanos =
                    200L
            )


        val snapshot =
            TeslaDashboardRepository
                .snapshot
                .value


        assertEquals(
            66.5f,
            snapshot
                .drive
                ?.speedFloat
        )

        assertEquals(
            25,
            snapshot
                .drive
                ?.power
        )

        assertEquals(
            81,
            snapshot
                .charge
                ?.batteryLevel
        )

        assertEquals(
            100L,
            snapshot
                .driveUpdatedAtNanos
        )

        assertEquals(
            200L,
            snapshot
                .chargeUpdatedAtNanos
        )
    }
}
