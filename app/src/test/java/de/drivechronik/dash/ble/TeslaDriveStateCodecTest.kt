package de.drivechronik.dash.ble

import com.tesla.generated.carserver.common.Common
import com.tesla.generated.carserver.server.CarServer
import com.tesla.generated.carserver.vehicle.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaDriveStateCodecTest {

    @Test
    fun buildsGetDriveStateRequest() {

        val bytes =
            TeslaDriveStateCodec
                .buildRequest()


        val action =
            CarServer
                .Action
                .parseFrom(bytes)


        assertTrue(
            action.hasVehicleAction()
        )


        assertTrue(
            action
                .vehicleAction
                .hasGetVehicleData()
        )


        assertTrue(
            action
                .vehicleAction
                .getVehicleData
                .hasGetDriveState()
        )
    }


    @Test
    fun decodesDriveStateResponse() {

        val shift =
            Vehicle
                .ShiftState
                .newBuilder()
                .setD(
                    Common
                        .Void
                        .getDefaultInstance()
                )
                .build()


        val drive =
            Vehicle
                .DriveState
                .newBuilder()
                .setShiftState(
                    shift
                )
                .setSpeedFloat(
                    62.5f
                )
                .setSpeed(
                    62
                )
                .setPower(
                    44
                )
                .setOdometerInHundredthsOfAMile(
                    123456
                )
                .build()


        val vehicleData =
            Vehicle
                .VehicleData
                .newBuilder()
                .setDriveState(
                    drive
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


        val decoded =
            TeslaDriveStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            62.5f,
            decoded.speedFloat
        )


        assertEquals(
            62L,
            decoded.speed
        )


        assertEquals(
            44,
            decoded.power
        )


        assertEquals(
            "D",
            decoded.shiftState
        )


        assertEquals(
            123456,
            decoded.odometerHundredthsOfMile
        )
    }


    @Test
    fun preservesMissingOptionalValues() {

        val drive =
            Vehicle
                .DriveState
                .newBuilder()
                .build()


        val response =
            CarServer
                .Response
                .newBuilder()
                .setVehicleData(
                    Vehicle
                        .VehicleData
                        .newBuilder()
                        .setDriveState(
                            drive
                        )
                )
                .build()


        val decoded =
            TeslaDriveStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            null,
            decoded.speedFloat
        )

        assertEquals(
            null,
            decoded.speed
        )

        assertEquals(
            null,
            decoded.power
        )

        assertEquals(
            null,
            decoded.odometerHundredthsOfMile
        )
    }
}
