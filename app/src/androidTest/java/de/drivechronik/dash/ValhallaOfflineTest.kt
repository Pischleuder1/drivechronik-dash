package de.drivechronik.dash

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.valhalla.valhalla.Valhalla
import com.valhalla.valhalla.config.ValhallaConfigFactory
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlinx.coroutines.runBlocking
import de.drivechronik.dash.routing.*

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

    @Test
    fun dashboardRoutingService(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val folder = File(context.filesDir, "routing")
        folder.mkdirs()
        val tiles = File(folder, "valhalla_tiles.tar")

        try {
            inst.context.assets.open(
                "valhalla_tiles_bremen.tar"
            ).use { input ->
                tiles.outputStream().use { input.copyTo(it) }
            }

            val result = calculateOfflineRoute(
                context,
                RoutePoint(53.0833, 8.8130),
                RoutePoint(53.0758, 8.8074)
            ).getOrThrow()

            assertTrue(result.distanceKm > 0.0)
            assertTrue(result.points.size >= 2)
            assertTrue(result.durationSeconds > 0.0)

            android.util.Log.i(
                "DashValhalla",
                "DASH ROUTE OK: " +
                    result.points.size + " GPS-Punkte"
            )
        } finally {
            tiles.delete()
        }
    }

    @Test
    fun andorraCompatibility(): Unit = runBlocking {
        val inst = InstrumentationRegistry.getInstrumentation()
        val context = inst.targetContext
        val folder = File(context.filesDir, "routing")
        folder.mkdirs()
        val tiles = File(folder, "valhalla_tiles.tar")

        try {
            inst.context.assets.open(
                "valhalla_tiles_andorra.tar"
            ).use { input ->
                tiles.outputStream().use { input.copyTo(it) }
            }

            val result = calculateOfflineRoute(
                context,
                RoutePoint(42.5078, 1.5211),
                RoutePoint(42.5100, 1.5400)
            ).getOrThrow()

            assertTrue(result.distanceKm > 0.0)
            assertTrue(result.points.size >= 2)
            assertTrue(result.durationSeconds > 0.0)

            android.util.Log.i(
                "DashValhalla",
                "ANDORRA ROUTE OK: " +
                    result.points.size + " GPS-Punkte"
            )
        } finally {
            tiles.delete()
        }
    }

    @Test
    fun prepareBremenTiles() {
        val inst = InstrumentationRegistry.getInstrumentation()
        val folder = File(inst.targetContext.filesDir, "routing")
        check(folder.exists() || folder.mkdirs())
        val tiles = File(folder, "valhalla_tiles.tar")
        inst.context.assets.open("valhalla_tiles_bremen.tar")
            .use { input ->
                tiles.outputStream().use { input.copyTo(it) }
            }
        assertTrue(tiles.length() > 1000000L)
    }

    @Test
    fun germanyRouteOffline(): Unit = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val tiles = File(
            context.filesDir, "routing/valhalla_tiles.tar"
        )
        assertTrue("Deutschlandkarte fehlt", tiles.isFile)

        val route = calculateOfflineRoute(
            context,
            RoutePoint(52.28, 8.68),
            RoutePoint(48.137, 11.575)
        ).getOrThrow()

        assertTrue(route.distanceKm in 450.0..900.0)
        assertTrue(route.durationSeconds > 0.0)
        assertTrue(route.points.size > 100)
        assertTrue("Keine Manöver", route.maneuvers.isNotEmpty())
        android.util.Log.i(
            "DashValhalla",
            "MANÖVER: ${route.maneuvers.size}"
        )
        route.maneuvers.take(10).forEach { m ->
            android.util.Log.i(
                "DashValhalla",
                "Typ=${m.type} | ${m.instruction} | " +
                    "${m.distanceKm} km | Index=${m.beginShapeIndex}"
            )
        }


        android.util.Log.i(
            "DashValhalla",
            "DEUTSCHLAND OK: " +
                route.distanceKm + " km, " +
                route.points.size + " GPS-Punkte"
        )
    }

    @Test
    fun germanyRerouteOffline(): Unit = runBlocking {
        val context = InstrumentationRegistry
            .getInstrumentation().targetContext

        val destination = RoutePoint(48.137, 11.575)

        val original = calculateOfflineRoute(
            context,
            RoutePoint(52.28, 8.68),
            destination
        ).getOrThrow()

        val updated = calculateOfflineRoute(
            context,
            RoutePoint(52.29, 8.70),
            original.destination!!
        ).getOrThrow()

        assertTrue(updated.points.size > 100)
        assertTrue(updated.maneuvers.isNotEmpty())
        assertEquals(destination, updated.destination)
    }
}
