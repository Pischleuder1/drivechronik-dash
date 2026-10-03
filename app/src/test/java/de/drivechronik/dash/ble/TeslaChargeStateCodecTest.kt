package de.drivechronik.dash.ble

import com.tesla.generated.carserver.common.Common
import com.tesla.generated.carserver.server.CarServer
import com.tesla.generated.carserver.vehicle.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaChargeStateCodecTest {

    @Test
    fun buildsGetChargeStateRequest() {

        val bytes =
            TeslaChargeStateCodec
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
                .hasGetChargeState()
        )
    }


    @Test
    fun decodesChargeStateResponse() {

        val chargingState =
            Vehicle
                .ChargeState
                .ChargingState
                .newBuilder()
                .setCharging(
                    Common
                        .Void
                        .getDefaultInstance()
                )
                .build()


        val charge =
            Vehicle
                .ChargeState
                .newBuilder()
                .setBatteryLevel(
                    73
                )
                .setUsableBatteryLevel(
                    71
                )
                .setBatteryRange(
                    191.25f
                )
                .setEstBatteryRange(
                    176.50f
                )
                .setIdealBatteryRange(
                    205.75f
                )
                .setChargerPower(
                    11
                )
                .setChargingState(
                    chargingState
                )
                .build()


        val vehicleData =
            Vehicle
                .VehicleData
                .newBuilder()
                .setChargeState(
                    charge
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
            TeslaChargeStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            73,
            decoded.batteryLevel
        )

        assertEquals(
            71,
            decoded.usableBatteryLevel
        )

        assertEquals(
            191.25f,
            decoded.batteryRange
        )

        assertEquals(
            176.50f,
            decoded.estimatedBatteryRange
        )

        assertEquals(
            205.75f,
            decoded.idealBatteryRange
        )

        assertEquals(
            11,
            decoded.chargerPower
        )

        assertEquals(
            "CHARGING",
            decoded.chargingState
        )
    }


    @Test
    fun preservesMissingOptionalValues() {

        val charge =
            Vehicle
                .ChargeState
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
                        .setChargeState(
                            charge
                        )
                )
                .build()


        val decoded =
            TeslaChargeStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            null,
            decoded.batteryLevel
        )

        assertEquals(
            null,
            decoded.usableBatteryLevel
        )

        assertEquals(
            null,
            decoded.batteryRange
        )

        assertEquals(
            null,
            decoded.estimatedBatteryRange
        )

        assertEquals(
            null,
            decoded.idealBatteryRange
        )

        assertEquals(
            null,
            decoded.chargerPower
        )
    }
}
