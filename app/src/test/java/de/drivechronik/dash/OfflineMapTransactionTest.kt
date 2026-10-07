package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files

@RunWith(RobolectricTestRunner::class)
class OfflineMapTransactionTest {

    @Test
    fun transactionPreservesVersions() {
        val original = OfflineMapTransaction(
            packageId = "de-map",
            previousHash = "a".repeat(64),
            previousVersion = "2026-10",
            newHash = "b".repeat(64),
            newVersion = "2026-11"
        )

        val restored = OfflineMapTransaction.fromJson(
            original.toJson()
        )

        assertEquals(original, restored)
    }
}
