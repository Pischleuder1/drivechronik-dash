package de.drivechronik.dash

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate

/**
 * Schematische Fahrspuransicht für TOURING.
 *
 * Keine echte Fahrspur- oder Fahrzeugerkennung.
 * Das dargestellte Fahrzeug ist rein grafisch.
 */
@Composable
internal fun TouringLaneView(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {

        val w = size.width
        val h = size.height

        val laneColor = Color(0xFF5C748B)
        val roadColor = Color(0xFF151C25)
        val carColor = Color(0xFFBFC8D2)

        // Dezenter Straßenbereich.
        val road = Path().apply {
            moveTo(w * 0.40f, 0f)
            lineTo(w * 0.60f, 0f)
            lineTo(w * 0.94f, h)
            lineTo(w * 0.06f, h)
            close()
        }

        drawPath(
            path = road,
            color = roadColor
        )

        // Linke Fahrspurbegrenzung.
        drawLine(
            color = laneColor,
            start = Offset(w * 0.40f, 0f),
            end = Offset(w * 0.06f, h),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Rechte Fahrspurbegrenzung.
        drawLine(
            color = laneColor,
            start = Offset(w * 0.60f, 0f),
            end = Offset(w * 0.94f, h),
            strokeWidth = 3f,
            cap = StrokeCap.Round
        )

        // Größeres, stilisiertes Tesla-T.
        // Rein dekorativ, keine Fahrzeugerkennung.
        val logoW = w * 0.32f
        val logoH = h * 0.40f

        val cx = w / 2f
        val cy = h * 0.66f

        // Dezenter Hintergrund.
        drawRoundRect(
            color = Color(0xFF202B36),
            topLeft = Offset(
                cx - logoW * 0.65f,
                cy - logoH * 0.60f
            ),
            size = Size(
                logoW * 1.30f,
                logoH * 1.20f
            ),
            cornerRadius =
                androidx.compose.ui.geometry.CornerRadius(
                    logoW * 0.22f
                )
        )

        // Obere Tesla-Kappe.
        val cap = Path().apply {
            moveTo(cx - logoW * 0.45f, cy - logoH * 0.29f)
            cubicTo(
                cx - logoW * 0.20f, cy - logoH * 0.52f,
                cx + logoW * 0.20f, cy - logoH * 0.52f,
                cx + logoW * 0.45f, cy - logoH * 0.29f
            )
            lineTo(cx + logoW * 0.39f, cy - logoH * 0.17f)
            cubicTo(
                cx + logoW * 0.12f, cy - logoH * 0.35f,
                cx - logoW * 0.12f, cy - logoH * 0.35f,
                cx - logoW * 0.39f, cy - logoH * 0.17f
            )
            close()
        }

        drawPath(cap, Color.White)

        // Mittelsteg mit leicht gerundeter Kontur.
        val stem = Path().apply {
            moveTo(cx - logoW * 0.13f, cy - logoH * 0.22f)
            quadraticTo(
                cx, cy - logoH * 0.30f,
                cx + logoW * 0.13f, cy - logoH * 0.22f
            )
            lineTo(cx + logoW * 0.075f, cy + logoH * 0.42f)
            quadraticTo(
                cx, cy + logoH * 0.49f,
                cx - logoW * 0.075f, cy + logoH * 0.42f
            )
            close()
        }

        drawPath(stem, Color.White)

    }
}
