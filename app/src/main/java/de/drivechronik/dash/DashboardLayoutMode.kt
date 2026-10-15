package de.drivechronik.dash

/**
 * Verfügbare Darstellungsvarianten des Dashboards.
 *
 * CLASSIC entspricht dem bisherigen Layout.
 */
internal enum class DashboardLayoutMode(
    val label: String,
    val description: String
) {
    CLASSIC(
        label = "CLASSIC",
        description = "Klassisches Dashboard"
    ),

    TOURING(
        label = "TOURING",
        description = "Größere Kartenansicht"
    ),

    TECHNIK(
        label = "TECHNIK",
        description = "Erweiterte Fahrzeugdaten"
    )
}
