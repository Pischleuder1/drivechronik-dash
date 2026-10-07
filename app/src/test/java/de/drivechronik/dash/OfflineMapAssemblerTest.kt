package de.drivechronik.dash

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflineMapAssemblerTest {

    private fun hash(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256")
            .digest(bytes)
            .joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

    @Test
    fun repairsInterruptedAssembly() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val first = "TEST_PART_A".toByteArray()
        val second = "TEST_PART_B".toByteArray()
        val complete = first + second
        val id = "test-assembly-${System.nanoTime()}"

        val pkg = RemoteMapPackage(
            id, "de", "Test", "map", "test",
            complete.size.toLong(), hash(complete),
            "test.pmtiles",
            listOf(
                RemoteMapPart("part00", first.size.toLong(),
                    hash(first), "https://example.com/a"),
                RemoteMapPart("part01", second.size.toLong(),
                    hash(second), "https://example.com/b")
            )
        )

        val dir = File(
            context.getExternalFilesDir(null),
            "map-downloads/$id/${pkg.sha256}"
        )
        assertTrue(dir.mkdirs() || dir.isDirectory)

        val previous = OfflineMapAssembler.freeSpace
        try {
            File(dir, "part00").writeBytes(first)
            File(dir, "part01").writeBytes(second)
            File(dir, "test.pmtiles.verified")
                .writeText("BESCHAEDIGT")
            File(dir, "test.pmtiles.assembling")
                .writeText("UNVOLLSTAENDIG")

            OfflineMapAssembler.freeSpace = {
                Long.MAX_VALUE
            }

            val result = kotlinx.coroutines.coroutineScope {
                val first = async {
                    OfflineMapAssembler.assemble(context, pkg)
                }
                val second = async {
                    OfflineMapAssembler.assemble(context, pkg)
                }
                val a = first.await()
                val b = second.await()
                assertEquals(a.absolutePath, b.absolutePath)
                a
            }

            assertArrayEquals(complete, result.readBytes())
            assertFalse(
                File(dir, "test.pmtiles.assembling").exists()
            )
        } finally {
            OfflineMapAssembler.freeSpace = previous
            dir.deleteRecursively()
        }
    }
}
