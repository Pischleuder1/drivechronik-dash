package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer

data class TeslaVehicleLightData(
    val headlightsOn: Boolean?,
    val highBeamOn: Boolean?,
    val hazardsOn: Boolean?
)

object TeslaVehicleStateCodec {

    fun buildRequest(): ByteArray {
        val getVehicleData =
            CarServer.GetVehicleData.newBuilder()
                .setGetVehicleState(
                    CarServer.GetVehicleState.getDefaultInstance()
                )
                .build()

        return CarServer.Action.newBuilder()
            .setVehicleAction(
                CarServer.VehicleAction.newBuilder()
                    .setGetVehicleData(getVehicleData)
                    .build()
            )
            .build()
            .toByteArray()
    }

    fun decodeResponse(payload: ByteArray): TeslaVehicleLightData? {
        val response = CarServer.Response.parseFrom(payload)

        if (!response.hasVehicleData()) return null

        val vehicleData = response.vehicleData

        if (!vehicleData.hasVehicleState()) return null

        val state = vehicleData.vehicleState

        return TeslaVehicleLightData(
            headlightsOn =
                if (state.hasHeadlightsOn()) state.headlightsOn else null,
            highBeamOn =
                if (state.hasHighbeamlightsOn())
                    state.highbeamlightsOn
                else null,
            hazardsOn =
                if (state.hasHazardsOn()) state.hazardsOn else null
        )
    }
}
