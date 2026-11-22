package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

private val MonitorPanel = Color(0xFF14202C)
private val MonitorMuted = Color(0xFF98AABB)
private val MonitorGreen = Color(0xFF48DB83)
private val MonitorOrange = Color(0xFFFFA347)
private val MonitorBlue = Color(0xFF55B9F5)

@Composable
internal fun TechnikEnergyMonitor(
    state: DashboardUiState,
    demoMode: Boolean,
    modifier: Modifier = Modifier
) {
    // LIVE_RESTENERGIE_SETTINGS_1082
    val context = LocalContext.current

    val capacitySettings = remember(context) {
        BatteryCapacitySettings(context)
    }

    var liveCapacityKwh by remember(capacitySettings) {
        mutableStateOf(capacitySettings.load())
    }

    DisposableEffect(capacitySettings) {
        val unsubscribe = capacitySettings.observe {
            liveCapacityKwh = it
        }

        onDispose {
            unsubscribe()
        }
    }

    val power = state.powerKw.takeIf { it.isFinite() }

    val battery = if (demoMode) {
        val capacity = state.usableCapacityKwh
        val remaining = state.remainingKwh

        if (
            capacity.isFinite() &&
            capacity > 0f &&
            remaining.isFinite()
        ) {
            (remaining / capacity * 100f)
                .coerceIn(0f, 100f)
                .roundToInt()
        } else null
    } else {
        state.batteryPercent?.takeIf { it in 0..100 }
    }

    val range = state.rangeKm.takeIf { it >= 0 }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MonitorPanel, RoundedCornerShape(12.dp))
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "ENERGIEFLUSS",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        val flowColor = when {
            power == null -> MonitorMuted
            power > 0f -> MonitorOrange
            power < 0f -> MonitorBlue
            else -> MonitorMuted
        }

        val direction = when {
            power == null -> "LEISTUNG UNBEKANNT"
            power > 0f -> "BATTERIE → ANTRIEB"
            power < 0f -> "ANTRIEB → BATTERIE"
            else -> "KEIN ENERGIEFLUSS"
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MonitorNode(
                title = "BATTERIE",
                value = battery?.let { "$it %" } ?: "— %",
                color = MonitorGreen,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = when {
                    power == null || power == 0f -> "—"
                    power > 0f -> "→"
                    else -> "←"
                },
                color = flowColor,
                fontSize = 22.sp
            )

            MonitorNode(
                title = "E-MOTOR",
                value = power?.let {
                    "${abs(it).roundToInt()} kW"
                } ?: "— kW",
                color = flowColor,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = when {
                    power == null || power == 0f -> "—"
                    power > 0f -> "→"
                    else -> "←"
                },
                color = flowColor,
                fontSize = 22.sp
            )

            MonitorNode(
                title = "ANTRIEB",
                value = if (power == null) "—" else "AKTIV",
                color = flowColor,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = direction,
            modifier = Modifier.fillMaxWidth(),
            color = flowColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF344453))
        )

        Text(
            text = "BATTERIEINFORMATIONEN",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )

        // TECHNIK_RESTENERGIE_1073
        // LIVE: Keine kWh ohne verlässliche Kapazität.
        // DEMO: Bereits vorhandene Restenergie verwenden.
        val remainingEnergy = if (demoMode) {
            state.remainingKwh.takeIf {
                it.isFinite() && it >= 0f &&
                    state.usableCapacityKwh.isFinite() &&
                    state.usableCapacityKwh > 0f
            }
        } else {
            // Nur berechnen, wenn beide Werte vorliegen.
            val capacity = liveCapacityKwh
            val soc = battery

            if (
                capacity != null &&
                BatteryCapacitySettings.isValid(capacity) &&
                soc != null
            ) {
                capacity * soc / 100f
            } else {
                null
            }
        }

        val energyText = remainingEnergy?.let {
            String.format(Locale.GERMANY, "%.2f kWh", it)
        } ?: "— kWh"

        MonitorValueRow(
            label = "Ladezustand",
            value = "${battery?.let { "$it %" } ?: "— %"} = $energyText"
        )

        MonitorValueRow(
            label = "Reichweite",
            value = range?.let { "$it km" } ?: "— km"
        )

        MonitorValueRow(
            label = "Antriebsleistung",
            value = power?.let {
                "${it.coerceAtLeast(0f).roundToInt()} kW"
            } ?: "— kW",
            color = MonitorOrange
        )

        MonitorValueRow(
            label = "Rekuperation",
            value = power?.let {
                "${abs(it.coerceAtMost(0f)).roundToInt()} kW"
            } ?: "— kW",
            color = MonitorBlue
        )
    }
}

@Composable
private fun MonitorNode(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = MonitorMuted,
            fontSize = 10.sp
        )

        Spacer(Modifier.height(2.dp))

        Text(
            text = value,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun MonitorValueRow(
    label: String,
    value: String,
    color: Color = Color.White
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MonitorMuted,
            fontSize = 10.sp
        )

        Text(
            text = value,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
