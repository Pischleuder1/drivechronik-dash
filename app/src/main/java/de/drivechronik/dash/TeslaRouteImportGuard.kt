package de.drivechronik.dash

/*
 * Entscheidet, ob ein Tesla-Navigationsziel
 * automatisch auf der Dash-Karte übernommen
 * werden darf.
 *
 * Keine Bluetooth- oder Fahrzeugbefehle.
 */
internal class TeslaRouteImportGuard {

    private var lastAttemptedTarget: Pair<Float, Float>? = null

    fun shouldImport(
        latitude: Float?,
        longitude: Float?,
        dashRouteActive: Boolean,
        calculationRunning: Boolean,
        importedPlanActive: Boolean
    ): Boolean {

        if (latitude == null || longitude == null) {
            lastAttemptedTarget = null
            return false
        }

        if (!latitude.isFinite() || !longitude.isFinite()) {
            return false
        }

        if (
            latitude !in -90f..90f ||
            longitude !in -180f..180f ||
            (latitude == 0f && longitude == 0f)
        ) {
            return false
        }

        if (
            dashRouteActive ||
            calculationRunning ||
            importedPlanActive
        ) {
            return false
        }

        val target = latitude to longitude

        if (target == lastAttemptedTarget) {
            return false
        }

        lastAttemptedTarget = target
        return true
    }
}
