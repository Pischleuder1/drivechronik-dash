package de.drivechronik.dash

internal data class ChargingStationSelection(
    val lat: Double,
    val lon: Double,
    val name: String,
    val operators: String?,
    val chargingPoints: Int?,
    val maxPowerKw: Double?,
    val connectors: String?,
    val street: String?,
    val houseNumber: String?,
    val postcode: String?,
    val city: String?,
    val supercharger: Boolean,
    val fast: Boolean
) {
    val address: String
        get() {
            val streetLine = listOfNotNull(
                street?.takeIf { it.isNotBlank() },
                houseNumber?.takeIf { it.isNotBlank() }
            ).joinToString(" ")

            val cityLine = listOfNotNull(
                postcode?.takeIf { it.isNotBlank() },
                city?.takeIf { it.isNotBlank() }
            ).joinToString(" ")

            return listOf(streetLine, cityLine)
                .filter { it.isNotBlank() }
                .joinToString(", ")
        }
}
