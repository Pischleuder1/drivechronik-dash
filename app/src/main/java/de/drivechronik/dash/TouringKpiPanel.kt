package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Rechte KPI-Spalte für TOURING.
 *
 * Ohne Scrollen, mit kompakten Navigationswerten.
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
            .background(Color(0x7310151B))
            .padding(
                start = 12.dp,
                end = 12.dp,
                top = 16.dp,
                bottom = 10.dp
            ),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        if (offlineSummary != null) {
            TouringKpiEntry(
                title = "ZIEL",
                value = NavigationSummaryState.destination
                    ?: "Navigationsziel",
                prominent = true,
                isDestination = true
            )

            fun offlineValue(label: String): String? =
                offlineSummary
                    .lineSequence()
                    .firstOrNull {
                        it.trimStart().startsWith(
                            "$label:",
                            ignoreCase = true
                        )
                    }
                    ?.substringAfter(':')
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }

            offlineValue("Entfernung")?.let {
                TouringKpiEntry(
                    title = "RESTSTRECKE",
                    value = it
                )
            }

            offlineValue("Zeit")?.let {
                TouringKpiEntry(
                    title = "FAHRZEIT",
                    value = it
                )
            }

            offlineValue("Ankunft")?.let {
                TouringKpiEntry(
                    title = "ANKUNFT",
                    value = it
                )
            }
        } else if (navigation != null) {
            TouringKpiEntry(
                title = "ZIEL",
                value = navigation.destination ?: "Aktive Route",
                prominent = true,
                isDestination = true
            )

            navigation.remainingDistanceKm?.let { km ->
                val distance = if (km < 10f) {
                    String.format(Locale.GERMANY, "%.1f km", km)
                } else {
                    "${km.roundToInt()} km"
                }

                TouringKpiEntry(
                    title = "RESTSTRECKE",
                    value = distance
                )
            }

            navigation.remainingMinutes?.let { minutes ->
                val remaining =
                    minutes.roundToInt().coerceAtLeast(0)

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
    valueColor: Color = Color.White,
    prominent: Boolean = false,
    isDestination: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        if (prominent) {
            Text(
                text = title,
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = value,
                color = valueColor,
                fontSize = if (isDestination) 15.sp else 20.sp,
                lineHeight = if (isDestination) 18.sp else 23.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = if (isDestination) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 10.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = value,
                    color = valueColor,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.20f),
            thickness = 0.5.dp
        )
    }
}
