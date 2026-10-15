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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TechnikBackground)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().weight(0.38f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TechnikPanelBox(
                modifier = Modifier.weight(1.05f).fillMaxHeight()
            ) {
                Text("GESCHWINDIGKEIT", color = TechnikMuted, fontSize = 12.sp)

                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.speed.roundToInt().toString(),
                            color = Color.White,
                            fontSize = 48.sp,
                            lineHeight = 52.sp,
                            fontWeight = FontWeight.Light
                        )
                        Text(
                            "km/h",
                            color = TechnikMuted,
                            fontSize = 13.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "FAHRSTUFE ${state.gear}",
                        color = TechnikBlue,
                        fontSize = 12.sp
                    )

                }
            }

            TechnikPanelBox(
                modifier = Modifier.weight(1.95f).fillMaxHeight()
            ) {
                Text("ANTRIEBSLEISTUNG", color = TechnikMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))

                val power = state.powerKw
                    .takeIf { it.isFinite() } ?: 0f

                TechnikBidirectionalPowerBar(
                    powerKw = power,
                    available = demoMode
                )

            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().weight(0.62f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TechnikPanelBox(
                modifier = Modifier.weight(1.05f).fillMaxHeight()
            ) {
                Text("BATTERIE / ENERGIE", color = TechnikMuted, fontSize = 12.sp)
                Spacer(Modifier.weight(1f))

                val capacity = state.usableCapacityKwh
                val remaining = state.remainingKwh
                val valid = capacity.isFinite() &&
                    remaining.isFinite() && capacity > 0f

                val percent = if (valid) {
                    (remaining / capacity * 100f)
                        .coerceIn(0f, 100f)
                        .roundToInt()
                } else {
                    0
                }

                Text(
                    if (demoMode) "$percent %" else "— %",
                    color = TechnikBlue,
                    fontSize = 36.sp,
                    lineHeight = 40.sp
                )

                Spacer(Modifier.height(5.dp))
                TechnikProgressBar(
                    if (demoMode) percent / 100f else 0f,
                    TechnikBlue
                )
                Spacer(Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("RESTENERGIE", color = TechnikMuted, fontSize = 10.sp)
                        Text(
                            if (demoMode)
                                String.format(Locale.GERMANY, "%.1f kWh", remaining)
                            else
                                "— kWh",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("REICHWEITE", color = TechnikMuted, fontSize = 10.sp)
                        Text(
                            if (demoMode) "${state.rangeKm} km" else "— km",
                            color = Color.White,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1.95f)
                    .fillMaxHeight()
                    .background(TechnikPanel, RoundedCornerShape(12.dp))
            ) {
                NavigationLayoutPlaceholder(
                    nightMode = nightMode,
                    curvedEdge = false,
                    modifier = Modifier.fillMaxSize()
                )

                if (state.speedLimit > 0) {
                    TechnikSpeedLimitSign(
                        limit = state.speedLimit,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 14.dp, end = 14.dp)
                    )
                }
            }
        }
    }
}


@Composable
private fun TechnikSpeedLimitSign(
    limit: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(49.dp)
            .background(Color.White, CircleShape)
            .border(5.dp, Color(0xFFD62828), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = limit.toString(),
            color = Color.Black,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun TechnikBidirectionalPowerBar(
    powerKw: Float,
    available: Boolean
) {
    val power = powerKw.coerceAtLeast(0f)
    val regen = abs(powerKw.coerceAtMost(0f))

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
                if (available) "${abs(powerKw).roundToInt()} kW" else "— kW",
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
