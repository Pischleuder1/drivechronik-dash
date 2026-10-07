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
class OfflineMapDownloaderTest {
    @Test
    fun verifiedPartIsReused() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val bytes = "TEST_DOWNLOAD".toByteArray()
        fun hash(b: ByteArray) =
            MessageDigest.getInstance("SHA-256")
                .digest(b)
                .joinToString("") { "%02x".format(it.toInt() and 255) }

        val sha = hash(bytes)
        val part = RemoteMapPart(
            name = "test.part00",
            sizeBytes = bytes.size.toLong(),
            sha256 = sha,
            url = "https://github.com/Pischleuder1/drivechronik-maps/releases/download/test/test.part00"
        )
        val pkg = RemoteMapPackage(
            id = "test-map",
            country = "de",
            countryName = "Test",
            kind = "map",
            version = "test",
            sizeBytes = bytes.size.toLong(),
            sha256 = sha,
            fileName = "test.pmtiles",
            parts = listOf(part)
        )

        val dir = File(
            context.getExternalFilesDir(null),
            "map-downloads/${pkg.id}/${pkg.sha256}"
        )
        assertTrue(dir.mkdirs() || dir.isDirectory)
        val cached = File(dir, part.name)
        cached.writeBytes(bytes)

        val result = OfflineMapDownloader.downloadPart(
            context, pkg, part
        ) { _, _ -> }

        assertEquals(cached.absolutePath, result.absolutePath)
        assertArrayEquals(bytes, result.readBytes())
    }
}
