package de.drivechronik.dash.diagnostics

import android.content.Context
import de.drivechronik.dash.ble.TeslaDashboardTelemetryMapper
import de.drivechronik.dash.ble.TeslaVehicleSnapshot
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TelemetryDiagnosticsLogger(
    context: Context
) {

    companion object {

        private const val RETENTION_MS =
            24L * 60L * 60L * 1000L

        private const val MAX_TOTAL_BYTES =
            20L * 1024L * 1024L

        private const val MAX_FILE_BYTES =
            5L * 1024L * 1024L
    }


    private val directory =
        File(
            context.getExternalFilesDir(null)
                ?: context.filesDir,
            "diagnostics"
        )


    private val sessionId =
        DateTimeFormatter
            .ofPattern(
                "yyyyMMdd-HHmmss"
            )
            .withZone(
                ZoneId.systemDefault()
            )
            .format(
                Instant.now()
            )


    private var part =
        1


    private var currentFile:
        File


    private var writeCount =
        0


    init {

        directory.mkdirs()

        /*
         * currentFile zuerst setzen.
         *
         * Dadurch kann prune() die aktuell für diese
         * Sitzung vorgesehene Datei sicher erkennen.
         */
        currentFile =
            createFile()

        prune()
    }


    fun log(
        snapshot: TeslaVehicleSnapshot,
        nowNanos: Long =
            System.nanoTime()
    ) {

        val telemetry =
            TeslaDashboardTelemetryMapper
                .map(
                    snapshot =
                        snapshot,

                    nowNanos =
                        nowNanos
                )


        val drive =
            snapshot.drive

        val charge =
            snapshot.charge

        val location =
            snapshot.location


        /*
         * Für die erste Fahrzeugprüfung besonders wichtig:
         *
         * Teslas dokumentiertes speed-Feld behandeln
         * wir derzeit als mph.
         */
        val speedKmh =
            if (
                TeslaDashboardTelemetryMapper
                    .isDriveFresh(
                        telemetry
                    )
            ) {

                drive
                    ?.speed
                    ?.takeIf {
                        it >= 0L
                    }
                    ?.toDouble()
                    ?.times(
                        1.609344
                    )
            }
            else {
                null
            }


        val json =
            JSONObject()


        put(
            json,
            "timestamp",
            Instant.now()
                .toString()
        )

        put(
            json,
            "schema",
            2
        )


        /*
         * DriveState
         */
        put(
            json,
            "driveAgeMillis",
            telemetry.driveAgeMillis
        )

        put(
            json,
            "speed",
            drive?.speed
        )

        put(
            json,
            "speedFloat",
            drive?.speedFloat
        )

        put(
            json,
            "speedKmhCalculated",
            speedKmh
        )

        put(
            json,
            "shiftState",
            drive?.shiftState
        )

        put(
            json,
            "powerRaw",
            drive?.power
        )


        /*
         * ChargeState
         */
        put(
            json,
            "chargeAgeMillis",
            telemetry.chargeAgeMillis
        )

        put(
            json,
            "batteryLevel",
            charge?.batteryLevel
        )

        put(
            json,
            "usableBatteryLevel",
            charge?.usableBatteryLevel
        )

        put(
            json,
            "batteryRangeRaw",
            charge?.batteryRange
        )

        put(
            json,
            "estimatedBatteryRangeRaw",
            charge?.estimatedBatteryRange
        )

        put(
            json,
            "idealBatteryRangeRaw",
            charge?.idealBatteryRange
        )


        /*
         * Location
         */
        put(
            json,
            "locationAgeMillis",
            telemetry.locationAgeMillis
        )

        put(
            json,
            "latitude",
            location?.latitude
        )

        put(
            json,
            "longitude",
            location?.longitude
        )

        put(
            json,
            "headingDegrees",
            location?.headingDegrees
        )


        /*
         * Tesla Navigation
         */
        put(
            json,
            "routeDestination",
            drive?.activeRouteDestination
        )

        put(
            json,
            "routeMinutesToArrival",
            drive?.activeRouteMinutesToArrival
        )

        put(
            json,
            "routeMilesToArrival",
            drive?.activeRouteMilesToArrival
        )

        put(
            json,
            "routeTrafficMinutesDelay",
            drive?.activeRouteTrafficMinutesDelay
        )

        put(
            json,
            "routeEnergyAtArrival",
            drive?.activeRouteEnergyAtArrival
        )


        /*
         * ========================================================
         * RAW TESLA DRIVE RESPONSE
         * ========================================================
         *
         * Nur im expliziten Diagnosemodus vorhanden.
         *
         * Damit können wir später unbekannte Protobuf-Felder
         * zwischen verschiedenen Navigationszuständen vergleichen.
         */
        put(
            json,
            "rawResponseLength",
            drive?.rawResponseBytes?.size
        )

        put(
            json,
            "rawDriveStateLength",
            drive?.rawDriveStateBytes?.size
        )

        put(
            json,
            "rawResponseHex",
            drive
                ?.rawResponseBytes
                ?.toHexString()
        )

        put(
            json,
            "rawDriveStateHex",
            drive
                ?.rawDriveStateBytes
                ?.toHexString()
        )


        rotateIfNecessary()


        currentFile.appendText(
            json.toString() +
                "\n",
            Charsets.UTF_8
        )


        writeCount++


        /*
         * Nicht bei jedem Datensatz das
         * Dateisystem durchsuchen.
         */
        if (
            writeCount %
                60 ==
                0
        ) {

            prune()
        }
    }


    fun path():
        String {

        return directory
            .absolutePath
    }


    private fun rotateIfNecessary() {

        if (
            currentFile.length() <
                MAX_FILE_BYTES
        ) {
            return
        }

        part++

        currentFile =
            createFile()

        prune()
    }


    private fun createFile():
        File {

        return File(
            directory,
            "telemetry-$sessionId-part$part.jsonl"
        )
    }


    private fun prune() {

        val files =
            directory
                .listFiles {
                    file ->

                    file.isFile &&
                        file.extension ==
                        "jsonl"
                }
                ?.sortedBy {
                    it.lastModified()
                }
                ?.toMutableList()
                ?: return


        val cutoff =
            System.currentTimeMillis() -
                RETENTION_MS


        files
            .filter {
                it.lastModified() <
                    cutoff
            }
            .forEach {
                it.delete()
            }


        val remaining =
            directory
                .listFiles {
                    file ->

                    file.isFile &&
                        file.extension ==
                        "jsonl"
                }
                ?.sortedBy {
                    it.lastModified()
                }
                ?.toMutableList()
                ?: return


        var total =
            remaining.sumOf {
                it.length()
            }


        for (
            file in remaining
        ) {

            if (
                total <=
                    MAX_TOTAL_BYTES
            ) {
                break
            }


            /*
             * Die aktuell beschriebene Datei
             * nicht während des Schreibens löschen.
             */
            if (
                file.absolutePath ==
                    currentFile.absolutePath
            ) {
                continue
            }


            val length =
                file.length()


            if (
                file.delete()
            ) {

                total -=
                    length
            }
        }
    }


    private fun ByteArray.toHexString():
        String {

        val digits =
            "0123456789abcdef"

        val result =
            CharArray(
                size * 2
            )

        forEachIndexed {
                index,
                value ->

            val v =
                value.toInt() and
                    0xff

            result[
                index * 2
            ] =
                digits[
                    v ushr 4
                ]

            result[
                index * 2 + 1
            ] =
                digits[
                    v and 0x0f
                ]
        }

        return String(
            result
        )
    }


    private fun put(
        json: JSONObject,
        key: String,
        value: Any?
    ) {

        json.put(
            key,
            value
                ?: JSONObject.NULL
        )
    }
}
