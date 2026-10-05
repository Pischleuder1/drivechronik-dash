package de.drivechronik.dash

import android.location.Location
import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
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
    onRoute: (OfflineRoute) -> Unit,
    phoneState: State<Location?>
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
    // Gemeinsames Navigations-GPS

    val scope = rememberCoroutineScope()
    var voiceBusy by remember { mutableStateOf(false) }
    var wakeRequest by remember { mutableIntStateOf(0) }
    var wakeSound by remember { mutableStateOf(false) }

    val voice = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { response ->
        voiceBusy = false
        if (response.resultCode == Activity.RESULT_OK) {
            val spoken = response.data
                ?.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS
                )?.firstOrNull().orEmpty()

            query = spoken
                .replaceFirst(
                    Regex("^drive\\s*chronik[, :]*",
                        RegexOption.IGNORE_CASE), "")
                .replaceFirst(
                    Regex("^(navigiere|fahre)( mich)? (nach|zu|zum|zur) ",
                        RegexOption.IGNORE_CASE), "")
                .trim()

            found = null
            if (query.length >= 3) {
                searching = true
                result = "Sprachziel wird gesucht ..."
                scope.launch {
                    try {
                        found = searchDestination(context, query)
                        result = found?.name ?: "Kein Ziel gefunden"
                    } catch (e: Exception) {
                        result = "Zielsuche fehlgeschlagen"
                    } finally {
                        searching = false
                    }
                }
            } else {
                result = "Keine Adresse erkannt"
            }
        }
    }

    LaunchedEffect(wakeRequest) {
        if (wakeRequest == 0) return@LaunchedEffect

        if (wakeSound) {
            val tone = android.media.ToneGenerator(
                android.media.AudioManager.STREAM_NOTIFICATION, 65
            )
            tone.startTone(
                android.media.ToneGenerator.TONE_PROP_BEEP, 120
            )
            kotlinx.coroutines.delay(200L)
            tone.release()
        }
        wakeSound = false

        // Vosk muss das Mikrofon vollständig freigeben.
        kotlinx.coroutines.delay(400L)

        val intent = Intent(
            RecognizerIntent.ACTION_RECOGNIZE_SPEECH
        ).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
        }
        try {
            voice.launch(intent)
        } catch (e: Exception) {
            voiceBusy = false
            result = "Spracherkennung nicht verfügbar"
        }
    }

    Row(
        horizontalArrangement =
            Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = { open = true },
            modifier = Modifier
                .size(44.dp)
                .background(
                    Color.Black,
                    RoundedCornerShape(10.dp)
                )
        ) {
            Text(
                "⌖",
                color = Color.White,
                fontSize = 30.sp
            )
        }

        DriveChronikWakeControl(
            suspended = voiceBusy,
            onWake = {
                open = true
                voiceBusy = true
                wakeSound = true
                wakeRequest++
            }
        )
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
                    TextButton(onClick = {
                        voiceBusy = true
                        wakeSound = false
                        wakeRequest++
                    }) {
                        Text("🎤 Adresse sprechen")
                    }

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
