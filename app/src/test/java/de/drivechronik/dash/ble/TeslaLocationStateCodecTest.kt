package de.drivechronik.dash.ble

import com.tesla.generated.carserver.common.Common
import com.tesla.generated.carserver.server.CarServer
import com.tesla.generated.carserver.vehicle.Vehicle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class TeslaLocationStateCodecTest {

    @Test
    fun buildsGetLocationStateRequest() {

        val bytes =
            TeslaLocationStateCodec
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
                .hasGetLocationState()
        )
    }


    @Test
    fun decodesLocationStateResponse() {

        val nativeType =
            Vehicle
                .LocationState
                .GPSCoordinateType
                .newBuilder()
                .setWGS(
                    Common
                        .Void
                        .getDefaultInstance()
                )
                .build()


        val location =
            Vehicle
                .LocationState
                .newBuilder()
                .setLatitude(
                    52.3001f
                )
                .setLongitude(
                    8.6001f
                )
                .setHeading(
                    275
                )
                .setGpsAsOf(
                    1_800_000_000L
                )
                .setNativeLocationSupported(
                    true
                )
                .setNativeLatitude(
                    52.3002f
                )
                .setNativeLongitude(
                    8.6002f
                )
                .setNativeType(
                    nativeType
                )
                .setGeoLatitude(
                    52.3003f
                )
                .setGeoLongitude(
                    8.6003f
                )
                .setGeoHeading(
                    274.5f
                )
                .setGeoElevation(
                    91.2f
                )
                .setGeoAccuracy(
                    2.5f
                )
                .setEstimatedGpsValid(
                    true
                )
                .setEstimatedToRawDistance(
                    3.7f
                )
                .setLocationName(
                    "Test location"
                )
                .build()


        val vehicleData =
            Vehicle
                .VehicleData
                .newBuilder()
                .setLocationState(
                    location
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
            TeslaLocationStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            52.3001f,
            decoded.latitude
        )

        assertEquals(
            8.6001f,
            decoded.longitude
        )

        assertEquals(
            275L,
            decoded.headingDegrees
        )

        assertEquals(
            1_800_000_000L,
            decoded.gpsAsOf
        )

        assertEquals(
            true,
            decoded.nativeLocationSupported
        )

        assertEquals(
            52.3002f,
            decoded.nativeLatitude
        )

        assertEquals(
            8.6002f,
            decoded.nativeLongitude
        )

        assertEquals(
            "WGS",
            decoded.nativeCoordinateType
        )

        assertEquals(
            52.3003f,
            decoded.geoLatitude
        )

        assertEquals(
            8.6003f,
            decoded.geoLongitude
        )

        assertEquals(
            274.5f,
            decoded.geoHeading
        )

        assertEquals(
            91.2f,
            decoded.geoElevation
        )

        assertEquals(
            2.5f,
            decoded.geoAccuracy
        )

        assertEquals(
            true,
            decoded.estimatedGpsValid
        )

        assertEquals(
            3.7f,
            decoded
                .estimatedToRawDistanceMeters
        )

        assertEquals(
            "Test location",
            decoded.locationName
        )
    }


    @Test
    fun preservesMissingOptionalValues() {

        val location =
            Vehicle
                .LocationState
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
                        .setLocationState(
                            location
                        )
                )
                .build()


        val decoded =
            TeslaLocationStateCodec
                .decodeResponse(
                    response.toByteArray()
                )


        assertEquals(
            null,
            decoded.latitude
        )

        assertEquals(
            null,
            decoded.longitude
        )

        assertEquals(
            null,
            decoded.headingDegrees
        )

        assertEquals(
            null,
            decoded.nativeLatitude
        )

        assertEquals(
            null,
            decoded.nativeLongitude
        )

        assertEquals(
            "TYPE_NOT_SET",
            decoded.nativeCoordinateType
        )

        assertEquals(
            null,
            decoded.geoLatitude
        )

        assertEquals(
            null,
            decoded.geoLongitude
        )
    }
}
