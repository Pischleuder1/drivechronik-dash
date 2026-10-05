package de.drivechronik.dash

import android.content.Context
import de.drivechronik.dash.routing.*
import kotlin.math.*

internal suspend fun findSimulationDetour(
    context: Context,
    original: OfflineRoute,
    start: RoutePoint,
    heading: Float
): OfflineRoute? {
    val angle = Math.toRadians(heading.toDouble())
    val latScale = 111_320.0
    val lonScale = latScale *
        cos(Math.toRadians(start.lat)).coerceAtLeast(0.01)

    for (forward in listOf(500.0, 800.0)) {
        for (side in listOf(300.0, -300.0, 450.0, -450.0)) {
            val north = forward * cos(angle) -
                side * sin(angle)
            val east = forward * sin(angle) +
                side * cos(angle)

            val target = RoutePoint(
                start.lat + north / latScale,
                start.lon + east / lonScale
            )

            if (target.lat !in -90.0..90.0 ||
                target.lon !in -180.0..180.0) continue

            val candidate = calculateOfflineRoute(
                context, start, target
            ).getOrNull() ?: continue

            if (validSimulationDetour(
                    original, candidate, start
                )) {
                return candidate
            }
        }
    }

    return null
}
