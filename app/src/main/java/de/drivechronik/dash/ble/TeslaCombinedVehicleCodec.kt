package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer

/**
 * Kombinierte Tesla-Fahrzeugdatenabfrage.
 *
 * Fordert DRIVE, GPS und Fahrzeugstatus gleichzeitig an.
 * Ob das Fahrzeug alle Bereiche gemeinsam beantwortet,
 * muss am realen Tesla verifiziert werden.
 */
object TeslaCombinedVehicleCodec {

    fun buildRequest(): ByteArray {

        val getVehicleData =
            CarServer.GetVehicleData.newBuilder()
                .setGetDriveState(
                    CarServer.GetDriveState.getDefaultInstance()
                )
                .setGetLocationState(
                    CarServer.GetLocationState.getDefaultInstance()
                )
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
}
