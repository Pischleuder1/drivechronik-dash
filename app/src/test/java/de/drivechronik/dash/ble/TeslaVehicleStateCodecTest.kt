package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer
import com.tesla.generated.carserver.vehicle.Vehicle
import org.junit.Assert.*
import org.junit.Test

class TeslaVehicleStateCodecTest {

    private fun response(
        state: Vehicle.CurrentVehicleState?
    ): ByteArray {
        val data = Vehicle.VehicleData.newBuilder()

        if (state != null) {
            data.setVehicleState(state)
        }

        return CarServer.Response.newBuilder()
            .setVehicleData(data.build())
            .build()
            .toByteArray()
    }

    @Test
    fun requestContainsVehicleStateQuery() {
        val action = CarServer.Action.parseFrom(
            TeslaVehicleStateCodec.buildRequest()
        )

        assertTrue(action.hasVehicleAction())
        assertTrue(action.vehicleAction.hasGetVehicleData())
        assertTrue(
            action.vehicleAction.getVehicleData
                .hasGetVehicleState()
        )
    }

    @Test
    fun absentVehicleStateReturnsNull() {
        assertNull(
            TeslaVehicleStateCodec.decodeResponse(
                response(null)
            )
        )
    }

    @Test
    fun missingFieldsRemainUnknown() {
        val decoded = TeslaVehicleStateCodec.decodeResponse(
            response(
                Vehicle.CurrentVehicleState
                    .getDefaultInstance()
            )
        )

        assertNotNull(decoded)
        assertNull(decoded!!.headlightsOn)
        assertNull(decoded.highBeamOn)
        assertNull(decoded.hazardsOn)
    }

    @Test
    fun explicitFalseIsNotUnknown() {
        val state = Vehicle.CurrentVehicleState.newBuilder()
            .setHeadlightsOn(false)
            .setHighbeamlightsOn(false)
            .setHazardsOn(false)
            .build()

        val decoded = TeslaVehicleStateCodec.decodeResponse(
            response(state)
        )

        assertNotNull(decoded)
        assertEquals(false, decoded!!.headlightsOn)
        assertEquals(false, decoded.highBeamOn)
        assertEquals(false, decoded.hazardsOn)
    }

    @Test
    fun activeLightsAndHazardsAreDecoded() {
        val state = Vehicle.CurrentVehicleState.newBuilder()
            .setHeadlightsOn(true)
            .setHighbeamlightsOn(true)
            .setHazardsOn(true)
            .build()

        val decoded = TeslaVehicleStateCodec.decodeResponse(
            response(state)
        )

        assertNotNull(decoded)
        assertEquals(true, decoded!!.headlightsOn)
        assertEquals(true, decoded.highBeamOn)
        assertEquals(true, decoded.hazardsOn)
    }
}
