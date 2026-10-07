package de.drivechronik.dash

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.size

@Composable
internal fun MapInfoIcon(type: String) {
    val blue = Color(0xFF65B9FF)

    Canvas(Modifier.size(26.dp)) {
        val k = size.width / 24f
        fun point(x: Float, y: Float) = Offset(x*k, y*k)

        when (type) {
            "wifi" -> {
                for (radius in listOf(9f, 6f, 3f)) {
                    drawArc(
                        blue, 225f, 90f, false,
                        topLeft = point(12f-radius, 14f-radius),
                        size = androidx.compose.ui.geometry.Size(
                            radius*2*k, radius*2*k),
                        style = Stroke(2f*k, cap=StrokeCap.Round)
                    )
                }
                drawCircle(blue, 1.5f*k, point(12f, 17f))
            }
            "map" -> {
                val lines = listOf(
                    2f to 5f, 9f to 2f, 16f to 5f,
                    22f to 2f, 22f to 19f, 16f to 22f,
                    9f to 19f, 2f to 22f, 2f to 5f
                )
                lines.zipWithNext().forEach { (a,b) ->
                    drawLine(blue, point(a.first,a.second),
                        point(b.first,b.second), 2f*k)
                }
                drawLine(blue,point(9f,2f),
                    point(9f,19f),2f*k)
                drawLine(blue,point(16f,5f),
                    point(16f,22f),2f*k)
            }
            "route" -> {
                drawLine(blue,point(5f,19f),
                    point(5f,9f),2f*k)
                drawLine(blue,point(5f,9f),
                    point(19f,9f),2f*k)
                drawLine(blue,point(19f,9f),
                    point(19f,5f),2f*k)
                drawCircle(blue,3f*k,point(5f,19f))
                drawCircle(blue,3f*k,point(19f,5f))
                drawCircle(Color(0xFF142335),1.3f*k,
                    point(5f,19f))
                drawCircle(Color(0xFF142335),1.3f*k,
                    point(19f,5f))
            }
        }
    }
}
