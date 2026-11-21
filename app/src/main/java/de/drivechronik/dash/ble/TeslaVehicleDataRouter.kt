package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer
import android.util.Log


data class TeslaVehicleDataUpdate(

    val drive:
        TeslaDriveStateData? = null,

    val charge:
        TeslaChargeStateData? = null,

    val location:
        TeslaLocationStateData? = null,

    // TESLA_LIGHT_ROUTER_896
    val lights:
        TeslaVehicleLightData? = null

) {

    val hasVehicleData: Boolean
        get() =
            drive != null ||
                charge != null ||
                location != null ||
                    lights != null
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

        // TESLA_LIGHT_DIAGNOSTICS_886
        // Ausschließlich entschlüsselte Statuswerte protokollieren.
        // Keine Rohdaten, Schlüssel oder Fahrzeugkennungen ausgeben.
        val lights =
            if (vehicleData.hasVehicleState()) {
                TeslaVehicleStateCodec.decodeResponse(payload)
            } else {
                null
            }

        if (lights != null) {
            Log.d(
                "DriveChronikLIGHT",
                "VEHICLE_STATE " +
                    "headlights=${lights?.headlightsOn} " +
                    "highBeam=${lights?.highBeamOn} " +
                    "hazards=${lights?.hazardsOn}"
            )
        }



        // TESLA_BATTERY_DIAGNOSTICS_966
        if (vehicleData.hasChargeState()) {
            val charge = vehicleData.chargeState

            // TESLA_BATTERY_LOG_SAFE_967
            // Android-Logging darf den Decoder nicht beeinflussen.
            runCatching {
                Log.d(
                "DriveChronikCHARGE",
                "BATTERY_FIELDS " +
                    "batteryLevel=" +
                    (if (charge.hasBatteryLevel())
                        charge.batteryLevel.toString()
                    else "absent") +
                    " usableBatteryLevel=" +
                    (if (charge.hasUsableBatteryLevel())
                        charge.usableBatteryLevel.toString()
                    else "absent")
                )
            }
        }

        return TeslaVehicleDataUpdate(

            lights = lights,

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
