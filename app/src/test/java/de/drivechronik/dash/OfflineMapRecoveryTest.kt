package de.drivechronik.dash

import org.robolectric.RuntimeEnvironment
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflineMapRecoveryTest {
    private val context: Context
        get() = RuntimeEnvironment.getApplication()

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

    @Test
    fun restoresBackupAfterInterruptedSwap() {
        val dir = File(
            context.cacheDir,
            "recovery-test-${System.nanoTime()}"
        )
        assertTrue(dir.mkdirs())

        val target = File(dir, "test.pmtiles")
        val backup = File(dir, "test.pmtiles.backup")
        val old = "OLD_MAP".toByteArray()
        val oldHash = hash(old)

        backup.writeBytes(old)

        OfflineMapJournal.write(
            target,
            OfflineMapTransaction(
                packageId = "test-map",
                previousHash = oldHash,
                previousVersion = "2026-10",
                newHash = hash("NEW_MAP".toByteArray()),
                newVersion = "2026-11"
            )
        )

        OfflineMapInstaller.recover(
            context, target, "test-map"
        )

        assertArrayEquals(old, target.readBytes())
        assertFalse(
            File(dir, "test.pmtiles.txn").exists()
        )
        assertEquals(
            oldHash,
            OfflineMapVersions.hash(context, "test-map")
        )
    }

    @Test
    fun keepsNewMapAfterInterruptedCommit() {
        val dir = File(
            context.cacheDir,
            "recovery-new-${System.nanoTime()}"
        )
        assertTrue(dir.mkdirs())

        val target = File(dir, "test.pmtiles")
        val old = "OLD_MAP".toByteArray()
        val fresh = "NEW_MAP".toByteArray()

        target.writeBytes(fresh)
        File(dir, "test.pmtiles.backup").writeBytes(old)

        OfflineMapJournal.write(
            target,
            OfflineMapTransaction(
                packageId = "test-new-map",
                previousHash = hash(old),
                previousVersion = "2026-10",
                newHash = hash(fresh),
                newVersion = "2026-11"
            )
        )

        OfflineMapInstaller.recover(
            context, target, "test-new-map"
        )

        assertArrayEquals(fresh, target.readBytes())
        assertEquals(
            "2026-11",
            OfflineMapVersions.version(
                context, "test-new-map"
            )
        )
        assertEquals(
            hash(fresh),
            OfflineMapVersions.hash(
                context, "test-new-map"
            )
        )
    }


    @Test
    fun rejectsCorruptBackup() {
        val dir = File(
            context.cacheDir,
            "recovery-corrupt-${System.nanoTime()}"
        )
        assertTrue(dir.mkdirs())

        val target = File(dir, "test.pmtiles")
        val backup = File(dir, "test.pmtiles.backup")
        backup.writeText("CORRUPTED")

        OfflineMapJournal.write(
            target,
            OfflineMapTransaction(
                packageId = "test-corrupt-map",
                previousHash = hash("OLD_MAP".toByteArray()),
                previousVersion = "2026-10",
                newHash = hash("NEW_MAP".toByteArray()),
                newVersion = "2026-11"
            )
        )

        try {
            OfflineMapInstaller.recover(
                context, target, "test-corrupt-map"
            )
            fail("Beschädigte Sicherung akzeptiert")
        } catch (e: IllegalStateException) {
            // Erwartet: SHA-256-Prüfung fehlgeschlagen
        }

        assertFalse(target.exists())
        assertTrue(backup.exists())
        assertNotNull(OfflineMapJournal.read(target))
    }

}
