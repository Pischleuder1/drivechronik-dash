package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflineMapCrashRecoveryTest {
    private fun sha(s: String) = MessageDigest.getInstance("SHA-256")
        .digest(s.toByteArray()).joinToString("") {
            "%02x".format(it.toInt() and 255)
        }

    @Test
    fun recoversAllSwapStagesKeepingTheCorrectBackup() {
        val context = RuntimeEnvironment.getApplication()
        for (stage in 0..2) {
            val dir = File(context.cacheDir,
                "crash-recovery-${System.nanoTime()}-$stage")
            assertTrue(dir.mkdirs())
            val target = File(dir, "germany.pmtiles")
            val backup = File(dir, "germany.pmtiles.backup")
            val rollback = File(dir, "germany.pmtiles.rollback")
            val staged = File(dir, "germany.pmtiles.new")
            val id = "crash-test-${System.nanoTime()}-$stage"
            try {
                val a = "TEST_A"
                val c = "TEST_C"
                val d = "TEST_D"
                // 0: A schon umbenannt; 1: D aktiv;
                // 2: D aktiv, Backup C bereits entfernt.
                rollback.writeText(a)
                if (stage != 2) backup.writeText(c)
                if (stage != 0) target.writeText(d)
                staged.writeText(d)
                OfflineMapJournal.write(target, OfflineMapTransaction(
                    packageId = id,
                    previousHash = sha(a), previousVersion = "A",
                    newHash = sha(d), newVersion = "D"
                ))
                OfflineMapInstaller.recover(context, target, id)
                assertEquals(if (stage == 0) a else d, target.readText())
                assertEquals(if (stage == 0) c else a, backup.readText())
                assertFalse(rollback.exists())
                assertFalse(staged.exists())
                assertNull(OfflineMapJournal.read(target))
                assertEquals(if (stage == 0) sha(a) else sha(d),
                    OfflineMapVersions.hash(context, id))
                // Zweiter Start darf keine Änderungen bewirken.
                OfflineMapInstaller.recover(context, target, id)
                assertEquals(if (stage == 0) c else a, backup.readText())
            } finally {
                dir.deleteRecursively()
            }
        }
    }
}
