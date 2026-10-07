package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class OfflineMapDownloadCleanupTest {

    @Test
    fun removesOnlyMatchingHashDirectory() {
        val ctx = RuntimeEnvironment.getApplication()
        val root = ctx.getExternalFilesDir(null)!!
            .resolve("map-downloads/de-map")

        val shaA = "a".repeat(64)
        val shaB = "b".repeat(64)

        val dirA = root.resolve(shaA)
        val dirB = root.resolve(shaB)

        try {
            assertTrue(dirA.mkdirs() || dirA.isDirectory)
            assertTrue(dirB.mkdirs() || dirB.isDirectory)

            dirA.resolve("germany.pmtiles.verified")
                .writeText("A")

            dirB.resolve("germany.pmtiles.verified")
                .writeText("B")

            assertTrue(
                OfflineMapDownloadCleanup.clean(
                    ctx,
                    "de-map",
                    shaA
                )
            )

            assertFalse(dirA.exists())
            assertTrue(dirB.isDirectory)
            assertTrue(
                dirB.resolve("germany.pmtiles.verified").isFile
            )
        } finally {
            root.deleteRecursively()
        }
    }
}
