package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Kompaktes Fahrpanel für das TOURING-Layout.
 *
 * Ausschließlich Darstellung.
 * Keine eigene BLE-Verbindung und keine Demodaten.
 */
@Composable
internal fun CompactDrivePanel(
    state: DashboardUiState,
    demoMode: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0x73080B10))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf("P", "R", "N", "D").forEach { gear ->
                val active = state.gear.equals(
                    gear,
                    ignoreCase = true
                )

                Text(
                    text = gear,
                    fontSize = 18.sp,
                    fontWeight = if (active)
                        FontWeight.Bold
                    else
                        FontWeight.Normal,
                    color = if (active)
                        Color.White
                    else
                        Color.White.copy(alpha = 0.65f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.speed.roundToInt().toString(),
                    fontSize = 76.sp,
                    lineHeight = 80.sp,
                    fontWeight = FontWeight.Light,
                    color = Color.White
                )
                Text(
                    text = "km/h",
                    fontSize = 18.sp,
                    color = Color.White.copy(alpha = 0.90f)
                )
            }

            SpeedLimitSign(
                limit = state.speedLimit
            )
        }

        TouringLaneView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp
                )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "◀",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (state.leftIndicator) Color(0xFF39D77A) else Color.Transparent,
                fontSize = 20.sp
            )

            Text(
                text = "☼",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (state.lowBeam) Color(0xFF39D77A) else Color.Transparent,
                fontSize = 23.sp
            )

            Text(
                text = "FERN",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (state.highBeam) Color(0xFF4F9DFF) else Color.Transparent,
                fontSize = 12.sp
            )

            Text(
                text = "▶",
                modifier = Modifier.weight(1f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = if (state.rightIndicator) Color(0xFF39D77A) else Color.Transparent,
                fontSize = 20.sp
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Touring: Abstand zur MapLibre-Quellenangabe.
                .offset(y = (-48).dp)
                .background(
                    Color(0x73151C25),
                    RoundedCornerShape(10.dp)
                )
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val percent =
                if (state.usableCapacityKwh > 0f) {
                    (
                        state.remainingKwh /
                            state.usableCapacityKwh * 100f
                    ).coerceIn(0f, 100f).roundToInt()
                } else {
                    0
                }

            Text(
                text = "RESTENERGIE / REICHWEITE",
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 11.sp,
                letterSpacing = 1.sp
            )

            Spacer(Modifier.height(5.dp))

            Text(
                // LIVE_BATTERY_TOURING_720
                text = (
                    if (demoMode) {
                        "$percent %"
                    } else {
                        state.batteryPercent
                            ?.takeIf { it in 0..100 }
                            ?.let { "$it %" }
                            ?: "— %"
                    }
                ) + " / " + (
                    if (state.rangeKm >= 0) {
                        "${state.rangeKm} km"
                    } else {
                        "— km"
                    }
                ),
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1
            )


        }
    }
}
