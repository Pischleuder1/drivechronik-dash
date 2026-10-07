package de.drivechronik.dash

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class OfflineInstalledIntegrityTest {

    @Test
    fun detectsCorruptedFile() {
        val file = File.createTempFile("map-test-", ".bin")
        try {
            file.writeText("ORIGINAL")

            val expected = MessageDigest
                .getInstance("SHA-256")
                .digest(file.readBytes())
                .joinToString("") {
                    "%02x".format(it.toInt() and 255)
                }

            assertEquals(
                expected,
                OfflineMapInstaller.sha256(file)
            )

            file.writeText("BESCHAEDIGT")

            assertNotEquals(
                expected,
                OfflineMapInstaller.sha256(file)
            )
        } finally {
            file.delete()
        }
    }
}
