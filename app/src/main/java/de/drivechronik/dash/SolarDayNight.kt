package de.drivechronik.dash

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.PI
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.roundToLong
import kotlin.math.sin

object SolarDayNight {

    /*
     * Nur Fallback, solange noch nie eine Tesla-Position
     * empfangen wurde. Ungefähre Mitte Deutschlands.
     *
     * Sobald Tesla LocationState vorhanden ist,
     * wird ausschließlich die Fahrzeugposition verwendet.
     */
    const val DEFAULT_LATITUDE =
        51.1657

    const val DEFAULT_LONGITUDE =
        10.4515

    /*
     * Sonnenuntergang:
     * Standard-Horizont inkl. Refraktion.
     */
    private const val SUNSET_ZENITH_DEGREES =
        90.833

    /*
     * Bürgerliche Morgendämmerung:
     * Sonne 6 Grad unter dem Horizont.
     *
     * Das entspricht dem gewünschten Tagesanbruch.
     */
    private const val CIVIL_DAWN_ZENITH_DEGREES =
        96.0


    fun isNight(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): Boolean {

        require(
            latitude in -90.0..90.0
        )

        require(
            longitude in -180.0..180.0
        )

        val date =
            now.toLocalDate()

        val dawn =
            solarEventUtc(
                date = date,
                latitude = latitude,
                longitude = longitude,
                sunrise = true,
                zenithDegrees =
                    CIVIL_DAWN_ZENITH_DEGREES
            )

        val sunset =
            solarEventUtc(
                date = date,
                latitude = latitude,
                longitude = longitude,
                sunrise = false,
                zenithDegrees =
                    SUNSET_ZENITH_DEGREES
            )

        val instant =
            now.toInstant()

        /*
         * Normalfall:
         *
         * Nacht:
         *   Mitternacht -> Morgendämmerung
         *   Sonnenuntergang -> Mitternacht
         *
         * Tag:
         *   Morgendämmerung -> Sonnenuntergang
         */
        if (
            dawn != null &&
            sunset != null
        ) {

            return instant.isBefore(dawn) ||
                !instant.isBefore(sunset)
        }

        /*
         * Sonderfall für sehr hohe Breitengrade
         * mit Polar-Tag / Polar-Nacht.
         */
        return solarElevationDegrees(
            now = now,
            latitude = latitude,
            longitude = longitude
        ) < -0.833
    }


    internal fun solarEventUtc(
        date: LocalDate,
        latitude: Double,
        longitude: Double,
        sunrise: Boolean,
        zenithDegrees: Double
    ): Instant? {

        val day =
            date.dayOfYear.toDouble()

        val gamma =
            2.0 * PI / 365.0 *
                (day - 1.0)

        val equationOfTime =
            229.18 *
                (
                    0.000075 +
                        0.001868 * cos(gamma) -
                        0.032077 * sin(gamma) -
                        0.014615 * cos(2.0 * gamma) -
                        0.040849 * sin(2.0 * gamma)
                    )

        val declination =
            0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) +
                0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) +
                0.001480 * sin(3.0 * gamma)

        val latitudeRadians =
            Math.toRadians(latitude)

        val zenithRadians =
            Math.toRadians(zenithDegrees)

        val numerator =
            cos(zenithRadians) -
                sin(latitudeRadians) *
                    sin(declination)

        val denominator =
            cos(latitudeRadians) *
                cos(declination)

        if (denominator == 0.0) {
            return null
        }

        val cosHourAngle =
            numerator /
                denominator

        /*
         * Kein Schnittpunkt mit dem angegebenen
         * Sonnenstand: Polar-Tag bzw. Polar-Nacht.
         */
        if (
            cosHourAngle < -1.0 ||
            cosHourAngle > 1.0
        ) {
            return null
        }

        val hourAngleDegrees =
            Math.toDegrees(
                acos(cosHourAngle)
            )

        /*
         * NOAA-Sonnenstandsformel.
         *
         * Das Ergebnis darf bewusst < 0 oder > 1440
         * Minuten sein. Damit werden Zeitzonen nahe
         * der Datumsgrenze korrekt abgebildet.
         */
        val utcMinutes =
            720.0 -
                4.0 * longitude -
                equationOfTime +
                if (sunrise) {
                    -4.0 * hourAngleDegrees
                }
                else {
                    4.0 * hourAngleDegrees
                }

        return date
            .atStartOfDay(
                ZoneOffset.UTC
            )
            .toInstant()
            .plusSeconds(
                (utcMinutes * 60.0)
                    .roundToLong()
            )
    }


    private fun solarElevationDegrees(
        now: ZonedDateTime,
        latitude: Double,
        longitude: Double
    ): Double {

        val day =
            now.dayOfYear.toDouble()

        val localHour =
            now.hour +
                now.minute / 60.0 +
                now.second / 3600.0

        val gamma =
            2.0 * PI / 365.0 *
                (
                    day -
                        1.0 +
                        (localHour - 12.0) /
                            24.0
                    )

        val equationOfTime =
            229.18 *
                (
                    0.000075 +
                        0.001868 * cos(gamma) -
                        0.032077 * sin(gamma) -
                        0.014615 * cos(2.0 * gamma) -
                        0.040849 * sin(2.0 * gamma)
                    )

        val declination =
            0.006918 -
                0.399912 * cos(gamma) +
                0.070257 * sin(gamma) -
                0.006758 * cos(2.0 * gamma) +
                0.000907 * sin(2.0 * gamma) -
                0.002697 * cos(3.0 * gamma) +
                0.001480 * sin(3.0 * gamma)

        val localMinutes =
            now.hour * 60.0 +
                now.minute +
                now.second / 60.0

        val timezoneHours =
            now.offset.totalSeconds /
                3600.0

        var trueSolarMinutes =
            localMinutes +
                equationOfTime +
                4.0 * longitude -
                60.0 * timezoneHours

        trueSolarMinutes =
            (
                (
                    trueSolarMinutes %
                        1440.0
                    ) +
                    1440.0
                ) %
                1440.0

        var hourAngleDegrees =
            trueSolarMinutes /
                4.0 -
                180.0

        if (hourAngleDegrees < -180.0) {
            hourAngleDegrees +=
                360.0
        }

        val hourAngleRadians =
            Math.toRadians(
                hourAngleDegrees
            )

        val latitudeRadians =
            Math.toRadians(latitude)

        val cosineZenith =
            (
                sin(latitudeRadians) *
                    sin(declination) +
                    cos(latitudeRadians) *
                    cos(declination) *
                    cos(hourAngleRadians)
                )
                .coerceIn(
                    -1.0,
                    1.0
                )

        val zenith =
            acos(cosineZenith)

        return 90.0 -
            Math.toDegrees(zenith)
    }
}
