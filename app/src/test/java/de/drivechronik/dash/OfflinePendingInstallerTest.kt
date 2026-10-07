package de.drivechronik.dash

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.security.MessageDigest

@RunWith(RobolectricTestRunner::class)
class OfflinePendingInstallerTest {

    @Test
    fun noPendingDoesNothing() = runBlocking {
        val ctx = RuntimeEnvironment.getApplication()
        assertNull(OfflineMapPending.pending(ctx, "de-routing"))
        assertFalse(
            OfflinePendingInstaller.process(ctx, "de-routing")
        )
    }

    @Test
    fun alreadyInstalledIsNotReplaced() = runBlocking {
        val ctx = RuntimeEnvironment.getApplication()
        val data = "TEST_ROUTING".toByteArray()
        val sha = MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }

        val id = "de-routing"
        val target = ctx.filesDir.resolve(
            "routing/valhalla_tiles.tar"
        )
        val catalog = ctx.filesDir.resolve(
            "offline-map-catalog.json"
        )
        val dir = ctx.getExternalFilesDir(null)!!
            .resolve("map-downloads/$id/$sha")
        val verified = dir.resolve(
            "valhalla_tiles.tar.verified"
        )

        assertFalse(target.exists())
        assertFalse(catalog.exists())
        assertNull(OfflineMapPending.pending(ctx, id))

        try {
            assertTrue(dir.mkdirs() || dir.isDirectory)
            assertTrue(target.parentFile!!.mkdirs() ||
                target.parentFile!!.isDirectory)

            target.writeBytes(data)
            verified.writeBytes(data)

            val part = RemoteMapPart(
                "test.part00", data.size.toLong(), sha,
                "https://github.com/Pischleuder1/drivechronik-maps/releases/download/test/test.part00"
            )
            val pkg = RemoteMapPackage(
                id, "de", "Deutschland", "routing",
                "test", data.size.toLong(), sha,
                "valhalla_tiles.tar", listOf(part)
            )

            catalog.writeText(
                org.json.JSONObject().apply {
                    put("schemaVersion", 1)
                    put("packages", org.json.JSONArray().put(
                        org.json.JSONObject().apply {
                            put("id", id)
                            put("country", "de")
                            put("countryName", "Deutschland")
                            put("kind", "routing")
                            put("version", "test")
                            put("sizeBytes", data.size)
                            put("sha256", sha)
                            put("fileName", pkg.fileName)
                            put("parts", org.json.JSONArray().put(
                                org.json.JSONObject().apply {
                                    put("name", part.name)
                                    put("sizeBytes", part.sizeBytes)
                                    put("sha256", sha)
                                    put("url", part.url)
                                }
                            ))
                        }
                    ))
                }.toString()
            )

            OfflineMapPending.mark(ctx, pkg, verified)

            assertTrue(OfflinePendingInstaller.process(ctx, id))
            assertArrayEquals(data, target.readBytes())
            assertNull(OfflineMapPending.pending(ctx, id))
            assertFalse(
                target.resolveSibling(
                    "valhalla_tiles.tar.backup"
                ).exists()
            )

            // Neuinstallation prüfen:
            // Vorgemerkte Datei ist gültig, Ziel fehlt.
            assertTrue(target.delete())
            assertTrue(dir.mkdirs() || dir.isDirectory)
            verified.writeBytes(data)
            OfflineMapPending.mark(ctx, pkg, verified)

            val oldSpace = OfflineMapInstaller.freeSpace
            try {
                OfflineMapInstaller.freeSpace = { Long.MAX_VALUE }

                assertTrue(
                    OfflinePendingInstaller.process(ctx, id)
                )
                assertArrayEquals(data, target.readBytes())
                assertNull(OfflineMapPending.pending(ctx, id))
                assertNull(OfflineMapJournal.read(target))
            } finally {
                OfflineMapInstaller.freeSpace = oldSpace
            }

            // PENDING_LOW_SPACE: Vormerkung muss erhalten bleiben.
            assertTrue(target.delete())
            assertTrue(dir.mkdirs() || dir.isDirectory)
            verified.writeBytes(data)
            OfflineMapPending.mark(ctx, pkg, verified)

            val savedSpace = OfflineMapInstaller.freeSpace
            try {
                OfflineMapInstaller.freeSpace = { 0L }

                val failure = runCatching {
                    OfflinePendingInstaller.process(ctx, id)
                }

                assertTrue(failure.isFailure)
                assertFalse(target.exists())
                assertEquals(
                    sha,
                    OfflineMapPending.pending(ctx, id)
                )
                assertNull(OfflineMapJournal.read(target))
            } finally {
                OfflineMapInstaller.freeSpace = savedSpace
            }


        } finally {
            OfflineMapPending.clearIfMatching(ctx, id, sha)
            target.delete()
            catalog.delete()
            dir.deleteRecursively()
        }
    }
}
