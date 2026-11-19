package de.drivechronik.dash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TeslaRouteImportGuardTest {

    @Test
    fun importsNewTeslaTargetOnlyOnce() {
        val guard = TeslaRouteImportGuard()

        assertTrue(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )

        assertFalse(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )
    }

    @Test
    fun protectsExistingDashNavigation() {
        val guard = TeslaRouteImportGuard()

        assertFalse(
            guard.shouldImport(52.3f, 8.6f, true, false, false)
        )

        assertFalse(
            guard.shouldImport(52.3f, 8.6f, false, true, false)
        )

        assertFalse(
            guard.shouldImport(52.3f, 8.6f, false, false, true)
        )

        assertTrue(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )
    }

    @Test
    fun acceptsChangedDestination() {
        val guard = TeslaRouteImportGuard()

        assertTrue(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )

        assertTrue(
            guard.shouldImport(53.1f, 9.2f, false, false, false)
        )
    }

    @Test
    fun rejectsInvalidCoordinates() {
        val guard = TeslaRouteImportGuard()

        assertFalse(
            guard.shouldImport(null, null, false, false, false)
        )

        assertFalse(
            guard.shouldImport(0f, 0f, false, false, false)
        )

        assertFalse(
            guard.shouldImport(100f, 8f, false, false, false)
        )
    }

    @Test
    fun clearingTeslaDestinationAllowsFutureImport() {
        val guard = TeslaRouteImportGuard()

        assertTrue(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )

        assertFalse(
            guard.shouldImport(null, null, false, false, false)
        )

        assertTrue(
            guard.shouldImport(52.3f, 8.6f, false, false, false)
        )
    }
}
