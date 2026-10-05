package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import de.drivechronik.dash.routing.*
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun NavigationSearch(
    onRoute: (OfflineRoute) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    var found by remember {
        mutableStateOf<DestinationResult?>(null)
    }
    var routing by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val (phoneState, _, _) = rememberPhoneWeatherLocation()

    val scope = rememberCoroutineScope()

    Button(onClick = { open = true }) {
        Text("Ziel")
    }

    if (open) {
        AlertDialog(
            modifier = Modifier.fillMaxWidth(0.72f)
                .widthIn(max = 680.dp),
            properties = DialogProperties(
                usePlatformDefaultWidth = false
            ),
            onDismissRequest = { if (!searching) open = false },
            title = { Text("Navigationsziel") },
            text = {
                Column {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = query,
                        onValueChange = { query = it },
                        label = { Text("Ort oder Adresse") },
                        singleLine = true
                    )
                    if (result.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(result)
                    }
                }
            },
            confirmButton = {
                Row {
                    TextButton(
                        enabled = !searching && !routing &&
                            found != null,
                        onClick = {
                            val target = found ?: return@TextButton
                            routing = true
                            scope.launch {
                                val gps = weatherCoordinates(phoneState.value)
                                val route = gps?.let {
                                    calculateOfflineRoute(
                                        context,
                                        RoutePoint(it.first, it.second),
                                        target.point
                                    )
                                }
                                result = when {
                                    gps == null -> "GPS fehlt"
                                    route == null -> "Route nicht verfügbar"
                                    route.isFailure ->
                                        "Offline-Route: " +
                                        (route.exceptionOrNull()?.message
                                            ?: "Berechnung fehlgeschlagen")
                                    else -> {
                                        val data = route.getOrThrow()
                                        onRoute(data)
                                    open = false
                                        String.format(
                                            Locale.GERMANY,
                                            "Route: %.1f km · %.0f Min",
                                            data.distanceKm,
                                            data.durationSeconds / 60.0
                                        )
                                    }
                                }
                                routing = false
                            }
                        }
                    ) {
                        Text("Route starten")
                    }
                }
                TextButton(
                    enabled = !searching && query.trim().length >= 3,
                    onClick = {
                        searching = true
                        found = null
                        result = "Suche läuft …"
                        scope.launch {
                            val destination = searchDestination(
                                context, query
                            )
                            found = destination
                            result = destination?.name
                                ?: "Kein Ziel gefunden"
                            searching = false
                        }
                    }
                ) {
                    Text(if (searching) "Suche …" else "Suchen")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !searching,
                    onClick = { open = false }
                ) {
                    Text("Schließen")
                }
            }
        )
    }
}
