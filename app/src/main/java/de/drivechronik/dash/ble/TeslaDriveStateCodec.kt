package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer

data class TeslaDriveStateData(

    val speedFloat: Float?,

    val speed: Long?,

    val power: Int?,

    val shiftState: String,

    val odometerHundredthsOfMile: Int?

)


object TeslaDriveStateCodec {

    fun buildRequest(): ByteArray {

        val getDriveState =
            CarServer
                .GetDriveState
                .getDefaultInstance()


        val getVehicleData =
            CarServer
                .GetVehicleData
                .newBuilder()
                .setGetDriveState(
                    getDriveState
                )
                .build()


        val vehicleAction =
            CarServer
                .VehicleAction
                .newBuilder()
                .setGetVehicleData(
                    getVehicleData
                )
                .build()


        val action =
            CarServer
                .Action
                .newBuilder()
                .setVehicleAction(
                    vehicleAction
                )
                .build()


        return action.toByteArray()
    }


    fun decodeResponse(
        bytes: ByteArray
    ): TeslaDriveStateData {

        val response =
            CarServer
                .Response
                .parseFrom(bytes)


        require(
            response.hasVehicleData()
        ) {
            "CarServer response contains no vehicle data"
        }


        val vehicleData =
            response.vehicleData


        require(
            vehicleData.hasDriveState()
        ) {
            "VehicleData contains no DriveState"
        }


        val drive =
            vehicleData.driveState


        val speedFloat =
            if (
                drive.hasSpeedFloat()
            ) {
                drive.speedFloat
            }
            else {
                null
            }


        val speed =
            if (
                drive.hasSpeed()
            ) {
                Integer.toUnsignedLong(
                    drive.speed
                )
            }
            else {
                null
            }


        val power =
            if (
                drive.hasPower()
            ) {
                drive.power
            }
            else {
                null
            }


        val odometer =
            if (
                drive
                    .hasOdometerInHundredthsOfAMile()
            ) {
                drive
                    .odometerInHundredthsOfAMile
            }
            else {
                null
            }


        val shift =
            drive
                .shiftState
                .typeCase
                .name


        return TeslaDriveStateData(
            speedFloat = speedFloat,
            speed = speed,
            power = power,
            shiftState = shift,
            odometerHundredthsOfMile =
                odometer
        )
    }
}
