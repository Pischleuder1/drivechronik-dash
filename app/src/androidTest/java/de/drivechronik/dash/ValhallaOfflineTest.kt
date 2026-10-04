package de.drivechronik.dash

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ValhallaOfflineTest {
    @Test
    fun bremenRouteOffline() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val tiles = File(context.cacheDir, "bremen-test.tar")

        try {
            inst.context.assets.open(
                "valhalla_tiles_bremen.tar"
            ).use { input ->
                tiles.outputStream().use { input.copyTo(it) }
            }

            val config = ValhallaConfigFactory
                .usingTileExtract(tiles.absolutePath)

            val request = """
                {
                  "locations": [
                    {"lat":53.0833,"lon":8.8130},
                    {"lat":53.0758,"lon":8.8074}
                  ],
                  "costing":"auto",
                  "directions_options":{"units":"kilometers"}
                }
            """.trimIndent()

            Valhalla(context, config).use { engine ->
                val json = JSONObject(engine.routeRaw(request))
                val km = json.getJSONObject("trip")
                    .getJSONObject("summary")
                    .getDouble("length")
                assertTrue(km > 0.0 && km < 30.0)
                android.util.Log.i(
                    "DashValhalla", "OFFLINE ROUTE OK: $km km"
                )
            }
        } finally {
            tiles.delete()
        }
    }
}
