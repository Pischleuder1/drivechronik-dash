package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OfflineIntegrityGateTest {
    @Test
    fun corruptedMapBlocksStartup() {
        val ctx = RuntimeEnvironment.getApplication()
        val id = "de-map"
        val file = ctx.getExternalFilesDir(null)!!
            .resolve("germany.pmtiles")

        // Vorhandene Testdaten nicht überschreiben.
        assertFalse(file.exists())
        val previous = OfflineMapVersions.hash(ctx, id)
        assertNull(previous)

        try {
            file.writeText("BESCHAEDIGT")
            OfflineMapVersions.saveVerified(
                ctx, id, "test", "a".repeat(64)
            )

            OfflineStartupGate.begin()

            runCatching {
                OfflineInstalledIntegrity.verify(ctx)
            }.onSuccess {
                OfflineStartupGate.success()
            }.onFailure {
                OfflineStartupGate.failure(
                    it.message ?: "Prüfung fehlgeschlagen"
                )
            }

            assertFalse(OfflineStartupGate.ready)
            assertNotNull(OfflineStartupGate.error)
        } finally {
            file.delete()
            ctx.getSharedPreferences(
                "offline_map_versions", 0
            ).edit()
                .remove("$id.version")
                .remove("$id.sha256")
                .commit()
            OfflineStartupGate.begin()
        }
    }
}
