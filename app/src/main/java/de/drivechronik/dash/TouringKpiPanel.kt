package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Schmale, vertikale KPI-Spalte ausschließlich für TOURING.
 * Nutzt die vorhandenen Dashboard- und Navigationsdaten.
 */
@Composable
internal fun TouringKpiPanel(
    state: DashboardUiState,
    offlineSummary: String?,
    demoMode: Boolean,
    modifier: Modifier = Modifier
) {
    val navigation = state.navigation

    Column(
        modifier = modifier
            .background(Color(0xE610151B))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TouringKpiEntry(
            title = "REICHWEITE",
            value = if (demoMode) "${state.rangeKm} km" else "— km"
        )

        if (offlineSummary != null) {
            TouringKpiEntry(
                title = "ZIEL",
                value = NavigationSummaryState.destination
                    ?: "Navigationsziel"
            )
            TouringKpiEntry(
                title = "ROUTENINFO",
                value = offlineSummary
            )
        } else if (navigation != null) {
            TouringKpiEntry(
                title = "ZIEL",
                value = navigation.destination ?: "Aktive Route"
            )

            navigation.remainingDistanceKm?.let { km ->
                val distance = if (km < 10f) {
                    String.format(
                        java.util.Locale.GERMANY,
                        "%.1f km",
                        km
                    )
                } else {
                    "${km.roundToInt()} km"
                }

                TouringKpiEntry(
                    title = "RESTSTRECKE",
                    value = distance
                )
            }

            navigation.remainingMinutes?.let { minutes ->
                val remaining = minutes.roundToInt().coerceAtLeast(0)

                TouringKpiEntry(
                    title = "FAHRZEIT",
                    value = "$remaining Min."
                )

                val eta = LocalTime.now()
                    .plusMinutes(remaining.toLong())
                    .format(DateTimeFormatter.ofPattern("HH:mm"))

                TouringKpiEntry(
                    title = "ANKUNFT",
                    value = "$eta Uhr"
                )
            }

            navigation.energyAtArrivalPercent?.let { percent ->
                TouringKpiEntry(
                    title = "AKKU BEI ANKUNFT",
                    value = "${percent.roundToInt().coerceIn(0, 100)} %"
                )
            }

            navigation.trafficDelayMinutes?.let { delay ->
                TouringKpiEntry(
                    title = "VERKEHR",
                    value = "+${delay.roundToInt().coerceAtLeast(0)} Min.",
                    valueColor = Color(0xFFFFB45E)
                )
            }

        }
    }
}

@Composable
private fun TouringKpiEntry(
    title: String,
    value: String,
    valueColor: Color = Color.White
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Text(
            text = title,
            color = Color(0xFF9AA8B6),
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = value,
            color = valueColor,
            fontSize = 16.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(0.dp))

        HorizontalDivider(
            color = Color(0xFF303943),
            thickness = 0.5.dp
        )
    }
}
