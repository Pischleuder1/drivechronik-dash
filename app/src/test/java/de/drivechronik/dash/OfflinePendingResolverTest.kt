package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OfflinePendingResolverTest {
    @Test
    fun rejectsMissingCatalogAndMismatchedPending() {
        val context = RuntimeEnvironment.getApplication()
        val id = "de-map"
        val catalog = context.filesDir.resolve(
            "offline-map-catalog.json"
        )
        val prefs = context.getSharedPreferences(
            "offline_map_pending", 0
        )

        // Vorhandene Daten dürfen nicht überschrieben werden.
        assertFalse(catalog.exists())
        assertNull(OfflineMapPending.pending(context, id))

        try {
            assertNull(
                OfflinePendingResolver.resolve(context, id)
            )

            prefs.edit()
                .putString("$id.sha256", "a".repeat(64))
                .putString("$id.version", "test")
                .commit()

            assertThrows(Exception::class.java) {
                OfflinePendingResolver.resolve(context, id)
            }
        } finally {
            prefs.edit()
                .remove("$id.sha256")
                .remove("$id.version")
                .commit()
        }
    }

    @Test
    fun resolvesValidOfflinePackage() {
        val ctx = RuntimeEnvironment.getApplication()
        val data = "TEST_MAP".toByteArray()
        val sha = java.security.MessageDigest.getInstance("SHA-256")
            .digest(data).joinToString("") {
                "%02x".format(it.toInt() and 255)
            }
        val id = "de-map"
        val catalog = ctx.filesDir.resolve("offline-map-catalog.json")
        val dir = ctx.getExternalFilesDir(null)!!
            .resolve("map-downloads/$id/$sha")
        val file = dir.resolve("germany.pmtiles.verified")

        assertFalse(catalog.exists())
        assertNull(OfflineMapPending.pending(ctx, id))
        try {
            assertTrue(dir.mkdirs() || dir.isDirectory)
            file.writeBytes(data)
            val part = RemoteMapPart(
                "test.part00", data.size.toLong(), sha,
                "https://github.com/Pischleuder1/drivechronik-maps/releases/download/test/test.part00"
            )
            val pkg = RemoteMapPackage(
                id, "de", "Deutschland", "map", "test",
                data.size.toLong(), sha, "germany.pmtiles",
                listOf(part)
            )
            val json = org.json.JSONObject().apply {
                put("schemaVersion", 1)
                put("packages", org.json.JSONArray().put(
                    org.json.JSONObject().apply {
                        put("id", id)
                        put("country", "de")
                        put("countryName", "Deutschland")
                        put("kind", "map")
                        put("version", "test")
                        put("sizeBytes", data.size)
                        put("sha256", sha)
                        put("fileName", "germany.pmtiles")
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
            }
            catalog.writeText(json.toString())
            OfflineMapPending.mark(ctx, pkg, file)

            val result = OfflinePendingResolver.resolve(ctx, id)
            assertEquals(pkg.sha256, result!!.first.sha256)
            assertEquals(file.canonicalFile, result.second.canonicalFile)
        } finally {
            OfflineMapPending.clearIfMatching(ctx, id, sha)
            catalog.delete()
            dir.deleteRecursively()
        }
    }

}
