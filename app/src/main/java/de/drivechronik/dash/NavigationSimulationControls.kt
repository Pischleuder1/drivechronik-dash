package de.drivechronik.dash

import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.platform.LocalConfiguration
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
    var developerMode by remember {
        mutableStateOf(false)
    }
    var openOfflineMaps by remember {
        mutableStateOf(false)
    }

    val offlineMapsRequest =
        OfflineUpdateState.openOfflineMapsRequest

    LaunchedEffect(offlineMapsRequest) {
        if (offlineMapsRequest > 0L) {
            developerMode = false
            openOfflineMaps = true
            open = true
        }
    }

    IconButton(
        onClick = {
            openOfflineMaps = false
            open = true
        },
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
        MaterialTheme(
            colorScheme = darkColorScheme(
                surface = Color(0xFF142335),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF21364B),
                onSurfaceVariant = Color(0xFFB8D9F0),
                primary = Color(0xFF65B9FF)
            )
        ) {
        AlertDialog(
            onDismissRequest = {
                openOfflineMaps = false
                open = false
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .widthIn(max = 500.dp),
            title = { Text("Navigation · Einstellungen") },
            text = {
                val maxHeight =
                    LocalConfiguration.current.screenHeightDp * 0.50f

                Box(
                    modifier = Modifier
                        .heightIn(max = maxHeight.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (developerMode) {
                        Column {
                            TextButton(
                                onClick = { developerMode = false }
                            ) {
                                Text("← Normale Einstellungen")
                            }
                            NavigationSimulationMenu(
                                onStop,
                                route = route,
                                close = { open = false }
                            )
                        }
                    } else {
                        NavigationSettingsHome(
                            route = route,
                            onStop = onStop,
                            close = {
                                openOfflineMaps = false
                                open = false
                            },
                            onDeveloperMode = {
                                developerMode = true
                            },
                            initialMapsOpen = openOfflineMaps
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        openOfflineMaps = false
                        open = false
                    }
                ) {
                    Text("Schließen")
                }
            }
        )
        }
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
        if (searchError) {
            Text(
                "Keine geeignete Alternativstraße gefunden.",
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp
            )
        }

        Text(
            "NAVIGATIONSTESTS",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp
        )

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
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
            ) {
                Text("Abbiegung", fontSize = 11.sp)
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
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(
                    horizontal = 8.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    if (searching) "Suche..." else "Fehlfahrt",
                    fontSize = 11.sp
                )
            }

            Button(
                onClick = { sim.toggleGpsOutage() },
                enabled = sim.active,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp),
                contentPadding = PaddingValues(
                    horizontal = 6.dp,
                    vertical = 0.dp
                )
            ) {
                Text(
                    if (sim.gpsOutage) "GPS ein"
                    else "GPS aus",
                    fontSize = 11.sp
                )
            }
        }
    }
}
