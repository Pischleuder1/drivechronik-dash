package de.drivechronik.dash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

@Composable
internal fun RealRoundaboutIndicator(
    graphic: RoundaboutGraphic
) {
    val ring = graphic.match.ring
    val route = graphic.traversal

    Box(
        modifier = Modifier.size(120.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (ring.size < 3) return@Canvas

            val centerLat = ring.map { it.lat }.average()
            val centerLon = ring.map { it.lon }.average()
            val correction = cos(Math.toRadians(centerLat))

            fun raw(index: Int): Offset {
                val p = ring[index]
                return Offset(
                    ((p.lon - centerLon) * correction).toFloat(),
                    (centerLat - p.lat).toFloat()
                )
            }

            val entryVector = raw(route.entry)
            val rotation = PI / 2 -
                atan2(
                    entryVector.y.toDouble(),
                    entryVector.x.toDouble()
                )

            val cs = cos(rotation).toFloat()
            val sn = sin(rotation).toFloat()

            fun rotated(v: Offset) = Offset(
                v.x * cs - v.y * sn,
                v.x * sn + v.y * cs
            )

            val maximum = ring.indices.maxOf {
                val v = raw(it)
                hypot(v.x, v.y).toDouble()
            }.toFloat().coerceAtLeast(0.0000001f)

            val scale = size.minDimension * 0.25f / maximum
            val center = Offset(
                size.width / 2,
                size.height / 2
            )

            fun point(index: Int): Offset {
                val v = rotated(raw(index))
                return Offset(
                    center.x + v.x * scale,
                    center.y + v.y * scale
                )
            }

            fun branch(
                index: Int,
                color: Color,
                outward: Boolean
            ): Offset {
                val start = point(index)
                val dx = start.x - center.x
                val dy = start.y - center.y
                val length = hypot(dx, dy)
                    .coerceAtLeast(0.001f)

                val extension = size.minDimension * 0.17f
                val end = Offset(
                    start.x + dx / length * extension,
                    start.y + dy / length * extension
                )

                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = 7.dp.toPx(),
                    cap = StrokeCap.Round
                )

                return if (outward) end else start
            }

            val muted = Color(0xFF87949F)
            val active = Color(0xFF65B9FF)

            // Kreisverkehrring anhand der OSM-Geometrie.
            val ringPath = Path()
            ringPath.moveTo(point(0).x, point(0).y)

            for (i in 1 until ring.size) {
                val p = point(i)
                ringPath.lineTo(p.x, p.y)
            }
            ringPath.close()

            drawPath(
                ringPath,
                muted,
                style = Stroke(
                    width = 8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            )

            // Echte Anschlusspositionen.
            for (index in route.orderedExits) {
                branch(index, muted, true)
            }

            // Einfahrt des Fahrzeugs.
            branch(route.entry, active, false)

            // Tatsächlich befahrener Teil des Rings.
            val drivePath = Path()
            val first = point(route.entry)
            drivePath.moveTo(first.x, first.y)

            var index = route.entry
            var count = 0

            while (index != route.exit && count < ring.size) {
                index = (index + 1) % ring.size
                val p = point(index)
                drivePath.lineTo(p.x, p.y)
                count++
            }

            if (index == route.exit) {
                drawPath(
                    drivePath,
                    active,
                    style = Stroke(
                        width = 10.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )
            }

            // Gewählte Ausfahrt mit Pfeil.
            val tip = branch(route.exit, active, true)
            val start = point(route.exit)

            val dx = tip.x - start.x
            val dy = tip.y - start.y
            val length = hypot(dx, dy)
                .coerceAtLeast(0.001f)

            val ux = dx / length
            val uy = dy / length
            val arrowSize = 12.dp.toPx()

            val left = Offset(
                tip.x - ux * arrowSize - uy * arrowSize * .65f,
                tip.y - uy * arrowSize + ux * arrowSize * .65f
            )
            val right = Offset(
                tip.x - ux * arrowSize + uy * arrowSize * .65f,
                tip.y - uy * arrowSize - ux * arrowSize * .65f
            )

            drawLine(
                active, left, tip,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                active, right, tip,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        Text(
            text = route.exitNumber.toString(),
            fontSize = 25.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}
