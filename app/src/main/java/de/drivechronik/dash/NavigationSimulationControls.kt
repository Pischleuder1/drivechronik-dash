package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import de.drivechronik.dash.routing.OfflineRoute

@Composable
internal fun NavigationSimulationControls(
    route: OfflineRoute?,
    onStop: () -> Unit
) {
    val sim = NavigationSimulation
    var open by remember { mutableStateOf(false) }

    IconButton(
        onClick = { open = true },
        modifier = Modifier
            .size(44.dp)
            .background(
                Color(0xEE142B40),
                RoundedCornerShape(10.dp)
            )
    ) {
        Text("⚙", color = Color.White, fontSize = 28.sp)
    }

    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 500.dp),
            title = { Text("Navigation · Einstellungen") },
            text = {
                NavigationSimulationMenu(onStop,
                    route = route,
                    close = { open = false }
                )
            },
            confirmButton = {
                TextButton(onClick = { open = false }) {
                    Text("Schließen")
                }
            }
        )
    }
}


@Composable
private fun NavigationSimulationMenu(onStop: () -> Unit,
    route: OfflineRoute?,
    close: () -> Unit
) {
    val sim = NavigationSimulation
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var searching by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf(false) }


    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("GPS-SIMULATOR", fontSize = 12.sp)

        if (!sim.active) {
            Button(
                onClick = {
                    route?.let { sim.start(it) }
                    close()
                },
                enabled = route != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("SIM START")
            }
        } else {
            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { sim.togglePause() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (sim.paused) "WEITER" else "PAUSE")
                }
                Button(
                    onClick = { sim.changeSpeed() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("${sim.speedKmh} km/h")
                }
                Button(
                    onClick = {
                        onStop()
                        close()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("STOPP")
                }
            }
        }


        if (searchError) {
            Text(
                "Keine geeignete Alternativstraße gefunden.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        HorizontalDivider()
        Text("NAVIGATIONSTESTS", fontSize = 12.sp)

        Row(
            horizontalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    sim.jumpToNextTurn()
                    close()
                },
                enabled = sim.active && !sim.gpsOutage,
                modifier = Modifier.weight(1f)
            ) {
                Text("Abbiegung", fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val original = route
                    val position = sim.fix
                    if (original != null && position != null &&
                        !searching) {
                        searching = true
                        searchError = false
                        sim.togglePause()
                        scope.launch {
                            val detour = findSimulationDetour(
                                context, original,
                                position.point,
                                position.heading ?: 0f
                            )
                            if (detour != null &&
                                sim.startDetour(detour)) {
                                close()
                            } else {
                                searchError = true
                                if (sim.active && sim.paused)
                                    sim.togglePause()
                            }
                            searching = false
                        }
                    }
                },
                enabled = sim.active && !sim.paused &&
                    !sim.gpsOutage && !searching &&
                    route != null,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (searching) "Suche..." else "Fehlfahrt",
                    fontSize = 12.sp
                )
            }

            Button(
                onClick = { sim.toggleGpsOutage() },
                enabled = sim.active,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    if (sim.gpsOutage) "GPS einschalten"
                    else "GPS ausschalten"
                )
            }
        }
    }
}
