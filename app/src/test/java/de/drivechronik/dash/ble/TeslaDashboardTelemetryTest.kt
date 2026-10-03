package de.drivechronik.dash.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test


class TeslaDashboardTelemetryTest {

    @Test
    fun mapsCompleteVehicleSnapshot() {

        val snapshot =
            TeslaVehicleSnapshot(

                drive =
                    TeslaDriveStateData(
                        speedFloat = 62.5f,
                        speed = 62L,
                        power = 44,
                        shiftState = "D",
                        odometerHundredthsOfMile =
                            123456,

                        activeRouteDestination =
                            "Hannover",

                        activeRouteMinutesToArrival =
                            31f,

                        activeRouteMilesToArrival =
                            26.1f,

                        activeRouteTrafficMinutesDelay =
                            4f,

                        activeRouteEnergyAtArrival =
                            54f
                    ),

                charge =
                    TeslaChargeStateData(
                        batteryLevel = 73,
                        usableBatteryLevel = 71,
                        batteryRange = 191.25f,
                        estimatedBatteryRange =
                            176.5f,
                        idealBatteryRange =
                            205.75f,
                        chargerPower = 11,
                        chargingState =
                            "CHARGING"
                    ),

                location =
                    TeslaLocationStateData(
                        latitude = 52.3001f,
                        longitude = 8.6001f,
                        headingDegrees = 275L,
                        gpsAsOf = null,
                        nativeLocationSupported =
                            true,
                        nativeLatitude =
                            52.3002f,
                        nativeLongitude =
                            8.6002f,
                        nativeCoordinateType =
                            "WGS",
                        correctedLatitude =
                            null,
                        correctedLongitude =
                            null,
                        geoLatitude =
                            null,
                        geoLongitude =
                            null,
                        geoHeading =
                            null,
                        geoElevation =
                            null,
                        geoAccuracy =
                            null,
                        estimatedGpsValid =
                            true,
                        estimatedToRawDistanceMeters =
                            null,
                        locationName =
                            null
                    ),

                driveUpdatedAtNanos =
                    8_000_000_000L,

                chargeUpdatedAtNanos =
                    5_000_000_000L,

                locationUpdatedAtNanos =
                    9_000_000_000L
            )


        val result =
            TeslaDashboardTelemetryMapper
                .map(
                    snapshot =
                        snapshot,
                    nowNanos =
                        10_000_000_000L
                )


        assertEquals(
            62.5f,
            result.speedRaw
        )

        assertEquals(
            44,
            result.powerRaw
        )

        assertEquals(
            "D",
            result.gear
        )


        assertEquals(
            "Hannover",
            result.activeRouteDestination
        )

        assertEquals(
            31f,
            result.activeRouteMinutesToArrival
        )

        assertEquals(
            26.1f,
            result.activeRouteMilesToArrival
        )

        assertEquals(
            4f,
            result.activeRouteTrafficMinutesDelay
        )

        assertEquals(
            54f,
            result.activeRouteEnergyAtArrival
        )

        assertEquals(
            73,
            result.batteryLevel
        )

        assertEquals(
            71,
            result.usableBatteryLevel
        )

        assertEquals(
            191.25f,
            result.batteryRangeRaw
        )

        assertEquals(
            176.5f,
            result.estimatedBatteryRangeRaw
        )

        assertEquals(
            205.75f,
            result.idealBatteryRangeRaw
        )

        assertEquals(
            52.3001f,
            result.latitude
        )

        assertEquals(
            8.6001f,
            result.longitude
        )

        assertEquals(
            275L,
            result.headingDegrees
        )

        assertEquals(
            2000L,
            result.driveAgeMillis
        )

        assertEquals(
            5000L,
            result.chargeAgeMillis
        )

        assertEquals(
            1000L,
            result.locationAgeMillis
        )
    }


    @Test
    fun speedFloatHasPriority() {

        val snapshot =
            TeslaVehicleSnapshot(
                drive =
                    TeslaDriveStateData(
                        speedFloat = 81.75f,
                        speed = 82L,
                        power = null,
                        shiftState = "D",
                        odometerHundredthsOfMile =
                            null
                    ),
                driveUpdatedAtNanos =
                    1L
            )


        val result =
            TeslaDashboardTelemetryMapper
                .map(
                    snapshot,
                    nowNanos = 1L
                )


        assertEquals(
            81.75f,
            result.speedRaw
        )
    }


    @Test
    fun integerSpeedIsFallback() {

        val snapshot =
            TeslaVehicleSnapshot(
                drive =
                    TeslaDriveStateData(
                        speedFloat = null,
                        speed = 82L,
                        power = 31,
                        shiftState = "D",
                        odometerHundredthsOfMile =
                            null
                    )
            )


        val result =
            TeslaDashboardTelemetryMapper
                .map(
                    snapshot
                )


        assertEquals(
            82.0f,
            result.speedRaw
        )
    }


    @Test
    fun emptySnapshotProducesEmptyTelemetry() {

        val result =
            TeslaDashboardTelemetryMapper
                .map(
                    TeslaVehicleSnapshot(),
                    nowNanos =
                        10_000_000_000L
                )


        assertNull(
            result.speedRaw
        )

        assertNull(
            result.powerRaw
        )

        assertNull(
            result.gear
        )

        assertNull(
            result.batteryLevel
        )

        assertNull(
            result.batteryRangeRaw
        )

        assertNull(
            result.latitude
        )

        assertNull(
            result.longitude
        )

        assertNull(
            result.headingDegrees
        )

        assertNull(
            result.driveAgeMillis
        )

        assertNull(
            result.chargeAgeMillis
        )

        assertNull(
            result.locationAgeMillis
        )
    }


    @Test
    fun futureTimestampDoesNotProduceNegativeAge() {

        val snapshot =
            TeslaVehicleSnapshot(
                driveUpdatedAtNanos =
                    20_000_000_000L
            )


        val result =
            TeslaDashboardTelemetryMapper
                .map(
                    snapshot =
                        snapshot,
                    nowNanos =
                        10_000_000_000L
                )


        assertEquals(
            0L,
            result.driveAgeMillis
        )
    }

    @Test
    fun freshDriveGearIsDisplayed() {

        val telemetry =
            TeslaDashboardTelemetry(
                gear = "D",
                driveAgeMillis = 2_999L
            )

        assertEquals(
            "D",
            TeslaDashboardTelemetryMapper
                .displayGear(
                    telemetry
                )
        )
    }


    @Test
    fun staleDriveGearIsNotDisplayed() {

        val telemetry =
            TeslaDashboardTelemetry(
                gear = "D",
                driveAgeMillis = 3_001L
            )

        assertNull(
            TeslaDashboardTelemetryMapper
                .displayGear(
                    telemetry
                )
        )
    }


    @Test
    fun verboseGearNameIsNormalized() {

        val telemetry =
            TeslaDashboardTelemetry(
                gear = "reverse",
                driveAgeMillis = 500L
            )

        assertEquals(
            "R",
            TeslaDashboardTelemetryMapper
                .displayGear(
                    telemetry
                )
        )
    }


    @Test
    fun freshNavigationIsConvertedToMetric() {

        val telemetry =
            TeslaDashboardTelemetry(
                activeRouteDestination =
                    "Hannover",

                activeRouteMinutesToArrival =
                    31f,

                activeRouteMilesToArrival =
                    10f,

                activeRouteTrafficMinutesDelay =
                    4f,

                activeRouteEnergyAtArrival =
                    54f,

                driveAgeMillis =
                    2_000L
            )

        val navigation =
            TeslaDashboardTelemetryMapper
                .activeNavigation(
                    telemetry
                )

        assertEquals(
            "Hannover",
            navigation?.destination
        )

        assertEquals(
            16.09344f,
            navigation?.remainingDistanceKm
                ?: -1f,
            0.0001f
        )

        assertEquals(
            31f,
            navigation?.remainingMinutes
        )

        assertEquals(
            4f,
            navigation?.trafficDelayMinutes
        )

        assertEquals(
            54f,
            navigation?.energyAtArrivalPercent
        )
    }


    @Test
    fun staleNavigationIsHidden() {

        val telemetry =
            TeslaDashboardTelemetry(
                activeRouteDestination =
                    "Hannover",

                activeRouteMinutesToArrival =
                    31f,

                driveAgeMillis =
                    3_001L
            )

        assertNull(
            TeslaDashboardTelemetryMapper
                .activeNavigation(
                    telemetry
                )
        )
    }


    @Test
    fun missingRouteProducesNoNavigation() {

        val telemetry =
            TeslaDashboardTelemetry(
                driveAgeMillis =
                    500L
            )

        assertNull(
            TeslaDashboardTelemetryMapper
                .activeNavigation(
                    telemetry
                )
        )
    }

}
