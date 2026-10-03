package de.drivechronik.dash.ble

import com.tesla.generated.carserver.server.CarServer


data class TeslaLocationStateData(

    val latitude: Float?,

    val longitude: Float?,

    val headingDegrees: Long?,

    val gpsAsOf: Long?,

    val nativeLocationSupported: Boolean?,

    val nativeLatitude: Float?,

    val nativeLongitude: Float?,

    val nativeCoordinateType: String,

    val correctedLatitude: Float?,

    val correctedLongitude: Float?,

    val geoLatitude: Float?,

    val geoLongitude: Float?,

    val geoHeading: Float?,

    val geoElevation: Float?,

    val geoAccuracy: Float?,

    val estimatedGpsValid: Boolean?,

    val estimatedToRawDistanceMeters: Float?,

    val locationName: String?

)


object TeslaLocationStateCodec {

    fun buildRequest(): ByteArray {

        val getLocationState =
            CarServer
                .GetLocationState
                .getDefaultInstance()


        val getVehicleData =
            CarServer
                .GetVehicleData
                .newBuilder()
                .setGetLocationState(
                    getLocationState
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
    ): TeslaLocationStateData {

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
            vehicleData.hasLocationState()
        ) {
            "VehicleData contains no LocationState"
        }


        val location =
            vehicleData.locationState


        return TeslaLocationStateData(

            latitude =
                if (
                    location.hasLatitude()
                ) {
                    location.latitude
                }
                else {
                    null
                },

            longitude =
                if (
                    location.hasLongitude()
                ) {
                    location.longitude
                }
                else {
                    null
                },

            headingDegrees =
                if (
                    location.hasHeading()
                ) {
                    Integer.toUnsignedLong(
                        location.heading
                    )
                }
                else {
                    null
                },

            gpsAsOf =
                if (
                    location.hasGpsAsOf()
                ) {
                    location.gpsAsOf
                }
                else {
                    null
                },

            nativeLocationSupported =
                if (
                    location
                        .hasNativeLocationSupported()
                ) {
                    location
                        .nativeLocationSupported
                }
                else {
                    null
                },

            nativeLatitude =
                if (
                    location.hasNativeLatitude()
                ) {
                    location.nativeLatitude
                }
                else {
                    null
                },

            nativeLongitude =
                if (
                    location.hasNativeLongitude()
                ) {
                    location.nativeLongitude
                }
                else {
                    null
                },

            nativeCoordinateType =
                location
                    .nativeType
                    .typeCase
                    .name,

            correctedLatitude =
                if (
                    location.hasCorrectedLatitude()
                ) {
                    location.correctedLatitude
                }
                else {
                    null
                },

            correctedLongitude =
                if (
                    location.hasCorrectedLongitude()
                ) {
                    location.correctedLongitude
                }
                else {
                    null
                },

            geoLatitude =
                if (
                    location.hasGeoLatitude()
                ) {
                    location.geoLatitude
                }
                else {
                    null
                },

            geoLongitude =
                if (
                    location.hasGeoLongitude()
                ) {
                    location.geoLongitude
                }
                else {
                    null
                },

            geoHeading =
                if (
                    location.hasGeoHeading()
                ) {
                    location.geoHeading
                }
                else {
                    null
                },

            geoElevation =
                if (
                    location.hasGeoElevation()
                ) {
                    location.geoElevation
                }
                else {
                    null
                },

            geoAccuracy =
                if (
                    location.hasGeoAccuracy()
                ) {
                    location.geoAccuracy
                }
                else {
                    null
                },

            estimatedGpsValid =
                if (
                    location.hasEstimatedGpsValid()
                ) {
                    location.estimatedGpsValid
                }
                else {
                    null
                },

            estimatedToRawDistanceMeters =
                if (
                    location
                        .hasEstimatedToRawDistance()
                ) {
                    location
                        .estimatedToRawDistance
                }
                else {
                    null
                },

            locationName =
                if (
                    location.hasLocationName()
                ) {
                    location.locationName
                }
                else {
                    null
                }
        )
    }
}
