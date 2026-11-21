package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaCombinedVehicleCodecTest {

    @Test
    fun combinedRequestContainsAllThreeStates() {

        val action =
            CarServer.Action.parseFrom(
                TeslaCombinedVehicleCodec.buildRequest()
            )

        assertTrue(action.hasVehicleAction())

        val vehicleAction = action.vehicleAction

        assertTrue(vehicleAction.hasGetVehicleData())

        val request = vehicleAction.getVehicleData

        assertTrue(request.hasGetDriveState())
        assertTrue(request.hasGetLocationState())
        assertTrue(request.hasGetVehicleState())
    }
}
