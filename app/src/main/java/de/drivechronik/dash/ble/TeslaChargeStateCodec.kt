package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer

data class TeslaChargeStateData(

    val batteryLevel: Int?,

    val usableBatteryLevel: Int?,

    val batteryRange: Float?,

    val estimatedBatteryRange: Float?,

    val idealBatteryRange: Float?,

    val chargerPower: Int?,

    val chargingState: String

)


object TeslaChargeStateCodec {

    fun buildRequest(): ByteArray {

        val getChargeState =
            CarServer
                .GetChargeState
                .getDefaultInstance()


        val getVehicleData =
            CarServer
                .GetVehicleData
                .newBuilder()
                .setGetChargeState(
                    getChargeState
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
    ): TeslaChargeStateData {

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
            vehicleData.hasChargeState()
        ) {
            "VehicleData contains no ChargeState"
        }


        val charge =
            vehicleData.chargeState


        return TeslaChargeStateData(

            batteryLevel =
                if (
                    charge.hasBatteryLevel()
                ) {
                    charge.batteryLevel
                }
                else {
                    null
                },

            usableBatteryLevel =
                if (
                    charge.hasUsableBatteryLevel()
                ) {
                    charge.usableBatteryLevel
                }
                else {
                    null
                },

            batteryRange =
                if (
                    charge.hasBatteryRange()
                ) {
                    charge.batteryRange
                }
                else {
                    null
                },

            estimatedBatteryRange =
                if (
                    charge.hasEstBatteryRange()
                ) {
                    charge.estBatteryRange
                }
                else {
                    null
                },

            idealBatteryRange =
                if (
                    charge.hasIdealBatteryRange()
                ) {
                    charge.idealBatteryRange
                }
                else {
                    null
                },

            chargerPower =
                if (
                    charge.hasChargerPower()
                ) {
                    charge.chargerPower
                }
                else {
                    null
                },

            chargingState =
                charge
                    .chargingState
                    .typeCase
                    .name
        )
    }
}
