package de.drivechronik.dash.routing

import org.junit.Test
import org.junit.Assert.*

class RouteSpeedTest {
    @Test fun compareSpeed() {
        val stream = requireNotNull(
            javaClass.getResourceAsStream("/real_route_points.csv")
        )
        val points = stream.bufferedReader().use {
            it.readLines().drop(1).map { line ->
                val v = line.split(",")
                RoutePoint(v[0].toDouble(), v[1].toDouble())
            }
        }
        val route = OfflineRoute(
            points, 100.0, 6000.0, emptyList()
        )
        val fixes = (0 until 120).map {
            points[it * 50]
        }

        fun run(indexed: Boolean): Double {
            val start = System.nanoTime()
            repeat(3) {
                fixes.forEach {
                    assertNotNull(calculateRouteProgress(
                        route, it, useSpatialIndex = indexed
                    ))
                }
            }
            return (System.nanoTime() - start) / 1e6
        }

        run(false)
        run(true)
        val full = run(false)
        val fast = run(true)
        println("FULL_MS=$full")
        println("INDEX_MS=$fast")
        println("FAKTOR=${full / fast}")
    }
}
