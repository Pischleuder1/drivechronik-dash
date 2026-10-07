package de.drivechronik.dash

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflineMapUpdateTest {

    private fun hash(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.toByteArray())
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

    @Test
    fun updatesFromAtoBtoC() = runBlocking {
        val previous = OfflineMapInstaller.freeSpace
        OfflineMapInstaller.freeSpace = { Long.MAX_VALUE }
        try {

        val context = RuntimeEnvironment.getApplication()
        val dir = context.getExternalFilesDir(null)!!
        val target = File(dir, "germany.pmtiles")

        // Niemals vorhandene Testdateien überschreiben.
        assertFalse(target.exists())
        assertFalse(File(dir, "germany.pmtiles.backup").exists())
        assertFalse(File(dir, "germany.pmtiles.new").exists())
        assertNull(OfflineMapJournal.read(target))

        for (version in listOf("A", "B", "C", "A")) {
            val content = "TEST_VERSION_$version"
            val verified = File(
                context.cacheDir, "update-$version.verified"
            )
            verified.writeText(content)

            val pkg = RemoteMapPackage(
                id = "de-map",
                country = "de",
                countryName = "Deutschland",
                kind = "map",
                version = version,
                sizeBytes = verified.length(),
                sha256 = hash(content),
                fileName = "germany.pmtiles",
                parts = emptyList()
            )

            OfflineMapInstaller.install(
                context, pkg, verified
            )

            assertEquals(content, target.readText())
            assertEquals(
                hash(content),
                OfflineMapVersions.hash(context, "de-map")
            )
            assertEquals(
                version,
                OfflineMapVersions.version(context, "de-map")
            )
        }

        val backup = File(dir, "germany.pmtiles.backup")
        val archived = File(
            dir, "germany.pmtiles.backup.archived-" +
                hash("TEST_VERSION_A")
        )

        assertEquals("TEST_VERSION_A", target.readText())
        assertEquals("TEST_VERSION_C", backup.readText())
        assertFalse(archived.exists())

        // LOW_SPACE_TEST: Update darf nichts verändern.
        val beforeMap = target.readBytes()
        val beforeBackup = backup.readBytes()

        val contentD = "TEST_VERSION_D"
        val verifiedD = File(context.cacheDir, "update-D.verified")
        verifiedD.writeText(contentD)

        val pkgD = RemoteMapPackage(
            "de-map", "de", "Deutschland", "map", "D",
            verifiedD.length(), hash(contentD),
            "germany.pmtiles", emptyList()
        )

        OfflineMapInstaller.freeSpace = { 0L }

        try {
            val failure = runCatching {
                OfflineMapInstaller.install(context, pkgD, verifiedD)
            }
            assertTrue(failure.isFailure)
            assertArrayEquals(beforeMap, target.readBytes())
            assertArrayEquals(beforeBackup, backup.readBytes())
            assertFalse(archived.exists())
            assertNull(OfflineMapJournal.read(target))
            assertFalse(File(dir, "germany.pmtiles.new").exists())
        } finally {
            OfflineMapInstaller.freeSpace = { Long.MAX_VALUE }
        }



        // ROLLBACK_TEST: Aktivierung von D scheitert.
        val originalActivate = OfflineMapInstaller.activateFile
        OfflineMapInstaller.activateFile = { _, _ -> false }

        try {
            val failure = runCatching {
                OfflineMapInstaller.install(context, pkgD, verifiedD)
            }
            assertTrue(failure.isFailure)
        } finally {
            OfflineMapInstaller.activateFile = originalActivate
        }

        // Auch eine Wiederherstellung beim Neustart prüfen.
        OfflineMapInstaller.recover(context, target, "de-map")

        assertEquals("TEST_VERSION_A", target.readText())
        assertTrue(backup.isFile)
        assertEquals("TEST_VERSION_C", backup.readText())
        assertNull(OfflineMapJournal.read(target))
        assertFalse(File(dir, "germany.pmtiles.new").exists())

        } finally {
            OfflineMapInstaller.freeSpace = previous
        }
    }
}
