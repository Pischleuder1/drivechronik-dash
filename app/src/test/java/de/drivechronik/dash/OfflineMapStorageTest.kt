package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflineMapStorageTest {

    private fun hash(b: ByteArray) =
        MessageDigest.getInstance("SHA-256")
            .digest(b).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

    @Test
    fun checksFreeSpaceAndCachedParts() {
        val context = RuntimeEnvironment.getApplication()
        val a = ByteArray(100) { 1 }
        val b = ByteArray(100) { 2 }
        val all = a + b
        val id = "storage-test-${System.nanoTime()}"

        val pkg = RemoteMapPackage(
            id, "de", "Test", "map", "test",
            200, hash(all), "test.pmtiles",
            listOf(
                RemoteMapPart("part00", 100, hash(a), "test-a"),
                RemoteMapPart("part01", 100, hash(b), "test-b")
            )
        )

        val dir = File(
            context.getExternalFilesDir(null),
            "map-downloads/$id/${pkg.sha256}"
        )
        val old = OfflineMapStorage.freeSpace
        val reserve = 3L * 1024 * 1024 * 1024

        try {
            assertTrue(dir.mkdirs() || dir.isDirectory)

            OfflineMapStorage.freeSpace = { reserve + 350 }
            assertThrows(IllegalStateException::class.java) {
                OfflineMapStorage.check(context, pkg)
            }

            // Ein geprüftes Teil spart 100 Bytes.
            File(dir, "part00").writeBytes(a)
            OfflineMapStorage.check(context, pkg)

            // Auch ein defektes Teil darf nicht zählen.
            File(dir, "part00").writeBytes(b)
            assertThrows(IllegalStateException::class.java) {
                OfflineMapStorage.check(context, pkg)
            }
        } finally {
            OfflineMapStorage.freeSpace = old
            dir.deleteRecursively()
        }
    }
}
