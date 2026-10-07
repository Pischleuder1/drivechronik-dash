package de.drivechronik.dash

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File
import java.net.InetSocketAddress
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
class OfflineMapHttpTest {
    @Test
    fun resumesInterruptedDownload() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val bytes = "DRIVECHRONIK_RESUME_TEST_123456789".toByteArray()
        val offset = 12
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(bytes).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

        val part = RemoteMapPart(
            "resume.part00", bytes.size.toLong(), sha,
            "https://github.com/Pischleuder1/drivechronik-maps/releases/download/test/resume.part00"
        )
        val pkg = RemoteMapPackage(
            "test-resume", "de", "Test", "map", "test",
            bytes.size.toLong(), sha, "test.pmtiles",
            listOf(part)
        )

        val dir = File(
            context.getExternalFilesDir(null),
            "map-downloads/${pkg.id}/${pkg.sha256}"
        )
        assertTrue(dir.mkdirs() || dir.isDirectory)
        File(dir, part.name).delete()
        File(dir, part.name + ".download")
            .writeBytes(bytes.copyOfRange(0, offset))

        val receivedRange = AtomicReference<String>()
        val server = HttpServer.create(
            InetSocketAddress("127.0.0.1", 0), 0
        )
        val badFirst = java.util.concurrent.atomic.AtomicBoolean(true)
        server.createContext("/corrupt") { exchange ->
            val bad = bytes.clone()
            bad[0] = (bad[0].toInt() xor 1).toByte()
            exchange.sendResponseHeaders(200, bad.size.toLong())
            exchange.responseBody.use { it.write(bad) }
        }
        server.createContext("/test") { exchange ->
            receivedRange.set(
                exchange.requestHeaders.getFirst("Range")
            )
            if (badFirst.getAndSet(false)) {
                exchange.sendResponseHeaders(200, -1)
                exchange.close()
                return@createContext
            }
            val remainder = bytes.copyOfRange(offset, bytes.size)
            exchange.responseHeaders.add(
                "Content-Range",
                "bytes $offset-${bytes.size - 1}/${bytes.size}"
            )
            exchange.sendResponseHeaders(206, remainder.size.toLong())
            exchange.responseBody.use { it.write(remainder) }
        }
        server.start()

        val oldConnection = OfflineMapDownloader.openConnection
        val oldSpace = OfflineMapDownloader.freeSpace
        try {
            OfflineMapDownloader.freeSpace = { Long.MAX_VALUE }
            OfflineMapDownloader.openConnection = {
                URL(
                    "http://127.0.0.1:${server.address.port}/test"
                ).openConnection() as HttpURLConnection
            }

            val rejected = runCatching {
                OfflineMapDownloader.downloadPart(
                    context, pkg, part
                ) { _, _ -> }
            }
            assertTrue(rejected.isFailure)
            assertArrayEquals(
                bytes.copyOfRange(0, offset),
                File(dir, part.name + ".download").readBytes()
            )

            val result = OfflineMapDownloader.downloadPart(
                context, pkg, part
            ) { _, _ -> }

            assertEquals("bytes=$offset-", receivedRange.get())
            assertArrayEquals(bytes, result.readBytes())
            assertFalse(
                File(dir, part.name + ".download").exists()
            )
            // Ein neuer Download mit falschem SHA-256.
            File(dir, part.name).delete()
            File(dir, part.name + ".download").delete()

            OfflineMapDownloader.openConnection = {
                URL("http://127.0.0.1:${server.address.port}/corrupt")
                    .openConnection() as HttpURLConnection
            }

            val corrupt = runCatching {
                OfflineMapDownloader.downloadPart(
                    context, pkg, part
                ) { _, _ -> }
            }
            assertTrue(corrupt.isFailure)
            assertFalse(File(dir, part.name).exists())

        } finally {
            OfflineMapDownloader.openConnection = oldConnection
            OfflineMapDownloader.freeSpace = oldSpace
            server.stop(0)
        }
    }
}
