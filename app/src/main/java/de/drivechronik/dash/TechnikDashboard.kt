package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val TechnikBackground = Color(0xFF080D14)
private val TechnikPanel = Color(0xFF14202C)
private val TechnikMuted = Color(0xFF98AABB)
private val TechnikOrange = Color(0xFFFFA347)
private val TechnikBlue = Color(0xFF55B9F5)

@Composable
internal fun TechnikDashboard(
    state: DashboardUiState,
    nightMode: Boolean,
    demoMode: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .background(TechnikBackground)
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // TECHNIK_SPEED_FULL_HEIGHT_1069
        TechnikPanelBox(
            modifier = Modifier
                .weight(1.05f)
                .fillMaxHeight()
        ) {
            Text(
                "GESCHWINDIGKEIT",
                color = TechnikMuted,
                fontSize = 12.sp
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.speed
                            .roundToInt()
                            .toString(),
                        color = Color.White,
                        fontSize = 64.sp,
                        lineHeight = 70.sp,
                        fontWeight = FontWeight.Light,
                        maxLines = 1
                    )

                    Text(
                        "km/h",
                        color = TechnikMuted,
                        fontSize = 17.sp
                    )
                }
            }

            // TECHNIK_SETTINGS_GEAR_1087
            // Bestehendes Navigations-Einstellungsmenü
            // auch ohne Navigationskarte erreichbar machen.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "FAHRSTUFE ${state.gear}",
                    color = TechnikBlue,
                    fontSize = 13.sp
                )

                NavigationSimulationControls(
                    route = null,
                    onStop = {}
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1.95f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TechnikPanelBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.30f)
            ) {
                Text(
                    "ANTRIEBSLEISTUNG",
                    color = TechnikMuted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.weight(1f))

                TechnikBidirectionalPowerBar(
                    powerKw = state.powerKw,
                    available = state.powerKw.isFinite()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.70f)
                    .background(
                        TechnikPanel,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    TechnikEnergyMonitor(
                        state = state,
                        demoMode = demoMode,
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                    )

                    TechnikPowerHistory(
                        powerKw = state.powerKw,
                        demoMode = demoMode,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun TechnikBidirectionalPowerBar(
    powerKw: Float,
    available: Boolean
) {
    val safePower =
        if (available && powerKw.isFinite()) powerKw else 0f

    val power = safePower.coerceAtLeast(0f)
    val regen = abs(safePower.coerceAtMost(0f))

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "REKUP",
                color = TechnikBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                if (available) "${abs(safePower).roundToInt()} kW" else "— kW",
                color = Color.White,
                fontSize = 19.sp
            )

            Text(
                "POWER",
                color = TechnikOrange,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(13.dp)
                .background(
                    Color(0xFF344453),
                    RoundedCornerShape(6.dp)
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (if (available) regen / 60f else 0f).coerceIn(0f, 1f)
                        )
                        .fillMaxHeight()
                        .background(TechnikBlue)
                )
            }

            Box(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight()
                    .background(Color.White)
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(
                            (if (available) power / 120f else 0f).coerceIn(0f, 1f)
                        )
                        .fillMaxHeight()
                        .background(TechnikOrange)
                )
            }
        }
    }
}

@Composable
private fun TechnikProgressBar(
    progress: Float,
    color: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .background(Color(0xFF344453), RoundedCornerShape(5.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .background(color, RoundedCornerShape(5.dp))
        )
    }
}

@Composable
private fun TechnikPanelBox(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .background(TechnikPanel, RoundedCornerShape(12.dp))
            .padding(8.dp),
        content = content
    )
}
