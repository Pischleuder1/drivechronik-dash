package de.drivechronik.dash

import java.util.Locale

internal fun mapCountryFlag(code: String): String {
    val c = code.uppercase(Locale.ROOT)

    if (c.length != 2 ||
        c.any { it !in 'A'..'Z' }) {
        return "🌍"
    }

    return buildString {
        c.forEach {
            appendCodePoint(
                0x1F1E6 + (it - 'A')
            )
        }
    }
}
