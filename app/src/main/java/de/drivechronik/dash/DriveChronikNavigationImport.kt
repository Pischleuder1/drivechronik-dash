package de.drivechronik.dash

import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject

internal data class ImportedNavigationStop(
    val type: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val targetSoc: Int? = null
)

internal data class ImportedNavigationPlan(
    val version: Int,
    val name: String,
    val destination: ImportedNavigationStop,
    val stops: List<ImportedNavigationStop>
)

internal object NavigationImportState {

    var pendingPlan by
        mutableStateOf<ImportedNavigationPlan?>(null)
        private set

    var activePlan by
        mutableStateOf<ImportedNavigationPlan?>(null)
        private set

    var activeTargetIndex by
        mutableStateOf(0)
        private set

    fun publish(
        plan: ImportedNavigationPlan
    ) {
        pendingPlan = plan
    }

    fun clear() {
        pendingPlan = null
    }

    fun activate(
        plan: ImportedNavigationPlan
    ) {
        activePlan = plan
        activeTargetIndex = 0
        pendingPlan = null
    }

    fun currentTarget():
        ImportedNavigationStop? {

        val plan =
            activePlan ?: return null

        return targets(plan)
            .getOrNull(
                activeTargetIndex
            )
    }

    fun advanceTarget():
        ImportedNavigationStop? {

        val plan =
            activePlan ?: return null

        val allTargets =
            targets(plan)

        val nextIndex =
            activeTargetIndex + 1

        if (
            nextIndex >=
                allTargets.size
        ) {
            return null
        }

        activeTargetIndex =
            nextIndex

        return allTargets[
            activeTargetIndex
        ]
    }

    fun stopActive() {
        activePlan = null
        activeTargetIndex = 0
    }

    private fun targets(
        plan: ImportedNavigationPlan
    ): List<ImportedNavigationStop> =
        plan.stops +
            plan.destination
}

internal object DriveChronikNavigationImport {

    fun decodePayload(
        payload: String
    ): Result<ImportedNavigationPlan> =
        runCatching {
            val decoded =
                Base64.decode(
                    payload,
                    Base64.URL_SAFE or
                        Base64.NO_WRAP or
                        Base64.NO_PADDING
                )

            val json =
                JSONObject(
                    decoded.toString(
                        Charsets.UTF_8
                    )
                )

            parsePlan(json)
        }

    private fun parsePlan(
        json: JSONObject
    ): ImportedNavigationPlan {

        val version =
            json.optInt(
                "version",
                1
            )

        require(version == 1) {
            "Nicht unterstützte Plan-Version: $version"
        }

        val destinationJson =
            json.getJSONObject(
                "destination"
            )

        val destination =
            parseStop(
                destinationJson,
                defaultType = "destination"
            )

        val stops =
            buildList {
                val array =
                    json.optJSONArray(
                        "stops"
                    )

                if (array != null) {
                    for (
                        i in 0 until array.length()
                    ) {
                        val item =
                            array.optJSONObject(i)
                                ?: continue

                        add(
                            parseStop(
                                item,
                                defaultType =
                                    "charging"
                            )
                        )
                    }
                }
            }

        return ImportedNavigationPlan(
            version = version,
            name =
                json.optString(
                    "name",
                    destination.name
                ).ifBlank {
                    destination.name
                },
            destination = destination,
            stops = stops
        )
    }

    private fun parseStop(
        json: JSONObject,
        defaultType: String
    ): ImportedNavigationStop {

        val lat =
            json.getDouble("lat")

        val lon =
            json.getDouble("lon")

        require(
            lat in -90.0..90.0
        ) {
            "Ungültiger Breitengrad"
        }

        require(
            lon in -180.0..180.0
        ) {
            "Ungültiger Längengrad"
        }

        val targetSoc =
            if (
                json.has("targetSoc") &&
                !json.isNull("targetSoc")
            ) {
                json.optInt(
                    "targetSoc"
                )
                    .takeIf {
                        it in 0..100
                    }
            } else {
                null
            }

        return ImportedNavigationStop(
            type =
                json.optString(
                    "type",
                    defaultType
                ).ifBlank {
                    defaultType
                },
            name =
                json.optString(
                    "name",
                    "Unbenannter Halt"
                ).ifBlank {
                    "Unbenannter Halt"
                },
            lat = lat,
            lon = lon,
            targetSoc = targetSoc
        )
    }
}
