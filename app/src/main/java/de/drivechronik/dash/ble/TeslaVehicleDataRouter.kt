package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer


data class TeslaVehicleDataUpdate(

    val drive:
        TeslaDriveStateData? = null,

    val charge:
        TeslaChargeStateData? = null,

    val location:
        TeslaLocationStateData? = null

) {

    val hasVehicleData: Boolean
        get() =
            drive != null ||
                charge != null ||
                location != null
}


object TeslaVehicleDataRouter {

    fun decode(
        payload: ByteArray
    ): TeslaVehicleDataUpdate {

        val response =
            CarServer
                .Response
                .parseFrom(payload)


        if (
            !response.hasVehicleData()
        ) {

            return TeslaVehicleDataUpdate()
        }


        val vehicleData =
            response.vehicleData


        return TeslaVehicleDataUpdate(

            drive =
                if (
                    vehicleData.hasDriveState()
                ) {
                    TeslaDriveStateCodec
                        .decodeResponse(
                            payload
                        )
                }
                else {
                    null
                },

            charge =
                if (
                    vehicleData.hasChargeState()
                ) {
                    TeslaChargeStateCodec
                        .decodeResponse(
                            payload
                        )
                }
                else {
                    null
                },

            location =
                if (
                    vehicleData.hasLocationState()
                ) {
                    TeslaLocationStateCodec
                        .decodeResponse(
                            payload
                        )
                }
                else {
                    null
                }
        )
    }
}
