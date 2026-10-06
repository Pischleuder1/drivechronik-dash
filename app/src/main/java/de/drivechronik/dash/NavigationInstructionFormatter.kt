package de.drivechronik.dash

internal data class FormattedManeuver(
    val action: String,
    val detail: String,
    val street: String,
    val roadNumber: String
)

internal fun formatManeuver(
    instruction: String
): FormattedManeuver {
    val text = instruction.trim()
    val roundabout = Regex(
        """(?i)\bKreisverkehr\b"""
    ).containsMatchIn(text)

    fun splitRoad(value: String): Pair<String, String> {
        val cleaned = value.trim()
            .trimEnd('.', '!', ' ')
            .replace(
                Regex("""(?i)^(die|den|das)\s+"""), ""
            )

        val number = Regex(
            """(?i)\b[ABLK]\s*-?\s*\d+[a-z]?\b"""
        ).findAll(cleaned).lastOrNull()

        if (number == null) return cleaned to ""

        val road = number.value.trim()
        val street = cleaned
            .removeRange(number.range)
            .trim()
            .trimEnd('/', '-', ',', ' ')

        return street to road
    }

    if (roundabout) {
        val exit = Regex(
            """(?i)\b(\d+)\.\s*Ausfahrt\b"""
        ).find(text)?.groupValues?.get(1)

        val destination = Regex(
            """(?i)\bauf\s+(.+?)\s+nehmen[.!]?$"""
        ).find(text)?.groupValues?.get(1).orEmpty()

        val (street, number) = splitRoad(destination)

        return FormattedManeuver(
            action = "Kreisverkehr",
            detail = exit?.let { "$it. Ausfahrt" }
                ?: text,
            street = street,
            roadNumber = number
        )
    }

    val turn = Regex(
        """(?i)\b(scharf rechts|scharf links|leicht rechts|leicht links|rechts|links)\s+abbiegen\b"""
    ).find(text)

    if (turn != null) {
        val direction = turn.groupValues[1]
        val action = direction.replaceFirstChar {
            it.uppercase()
        } + " abbiegen"

        val destination = Regex(
            """(?i)\bauf\s+(.+)$"""
        ).find(text)?.groupValues?.get(1).orEmpty()

        val (street, number) = splitRoad(destination)

        return FormattedManeuver(
            action = action,
            detail = "",
            street = street,
            roadNumber = number
        )
    }

    // Unbekannte Anweisungen niemals abschneiden.
    return FormattedManeuver(
        action = text,
        detail = "",
        street = "",
        roadNumber = ""
    )
}
