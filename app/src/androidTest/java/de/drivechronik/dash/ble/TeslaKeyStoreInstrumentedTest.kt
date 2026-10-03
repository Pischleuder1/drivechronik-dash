package de.drivechronik.dash.ble

import androidx.test.ext.junit.runners.AndroidJUnit4
import java.security.interfaces.ECPublicKey
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TeslaKeyStoreInstrumentedTest {

    @Test
    fun createsAndReloadsTeslaKeyPair() {

        val first =
            TeslaKeyStore
                .getOrCreateKeyPair()

        assertNotNull(
            first.private
        )

        assertTrue(
            first.public is ECPublicKey
        )

        val firstEncoded =
            TeslaKeyStore
                .encodedPublicKey()

        assertEquals(
            65,
            firstEncoded.size
        )

        assertEquals(
            0x04,
            firstEncoded[0]
                .toInt() and 0xff
        )

        assertTrue(
            TeslaKeyStore.hasKey()
        )

        /*
         * Noch einmal aus AndroidKeyStore laden.
         * Es muss derselbe Public Key sein.
         */
        val secondEncoded =
            TeslaKeyStore
                .encodedPublicKey()

        assertArrayEquals(
            firstEncoded,
            secondEncoded
        )

        /*
         * AndroidKeyStore-Private-Keys dürfen
         * nicht als Schlüsselmaterial exportiert
         * werden.
         */
        assertEquals(
            null,
            first.private.encoded
        )
    }
}
