package de.drivechronik.dash

import android.location.Location

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
    onStop: () -> Unit,
    navigationActive: Boolean,
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
    var startRequest by remember { mutableIntStateOf(0) }

    val prompt = remember(context) {
        NavigationVoicePrompt(context)
    }
    val reply = remember(context) {
        NavigationVoiceReply(context)
    }

    DisposableEffect(prompt, reply) {
        onDispose {
            reply.cancel()
            prompt.close()
        }
    }



    val processSpeech: (String) -> Unit =
        processSpeech@{ spoken ->
            voiceBusy = false
            run {
val command = spoken
                    .replaceFirst(
                        Regex(
                            """^(drive\s*chronik|drei\s*chronik)[\s,:]*""",
                            RegexOption.IGNORE_CASE
                        ), ""
                    )
                    .trim()
                    .lowercase(Locale.GERMANY)
                    .trimEnd('.', '!', '?')

                when (command) {
                    "navigation stoppen", "route stoppen",
                    "navigation beenden", "route beenden",
                    "stoppen" -> {
                        if (navigationActive) onStop()
                        found = null
                        result = ""
                        open = false
                        return@processSpeech
                    }

                    "navigation starten", "route starten",
                    "starten", "ja" -> {
                        if (!navigationActive &&
                            found != null &&
                            !searching && !routing) {
                            startRequest++
                        } else {
                            result = "Keine startbereite Route"
                            open = true
                        }
                        return@processSpeech
                    }

                    "abbrechen", "nein", "schließen" -> {
                        open = false
                        found = null
                        result = ""
                        return@processSpeech
                    }
                }

                open = true

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

                        val destination = found
                        if (destination != null) {
                            voiceBusy = true
                            prompt.speak(
                                "${destination.name} gefunden. Navigation starten?"
                            ) {
                                reply.listen { answer ->
                                    voiceBusy = false
                                    val normalized = answer
                                        ?.trim()
                                        ?.lowercase(Locale.GERMANY)
                                        ?.trimEnd('.', '!', '?')

                                    when (normalized) {
                                        "ja", "starten",
                                        "navigation starten",
                                        "route starten" -> {
                                            startRequest++
                                        }
                                        "nein", "abbrechen" -> {
                                            found = null
                                            result = ""
                                            open = false
                                        }
                                        null -> {
                                            result = "Keine Antwort. Bitte manuell starten."
                                        }
                                        else -> {
                                            result = "Antwort nicht erkannt: $answer"
                                        }
                                    }
                                }
                            }
                        }
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
            val player = android.media.MediaPlayer.create(
                context,
                R.raw.drivechronik_wake
            )
            player?.setOnCompletionListener {
                it.release()
            }
            player?.start()
            kotlinx.coroutines.delay(450L)
        }
        wakeSound = false

        // Vosk muss das Mikrofon vollständig freigeben.
        kotlinx.coroutines.delay(400L)

        reply.listen { spoken ->
            if (spoken.isNullOrBlank()) {
                voiceBusy = false
                result = "Keine Sprache erkannt"
            } else {
                processSpeech(spoken)
            }
        }
    }


    LaunchedEffect(startRequest) {
        if (startRequest == 0) return@LaunchedEffect

                            val target = found ?: return@LaunchedEffect
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
                                        NavigationSummaryState.updateDestination(target.name)
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
                        onClick = { startRequest++ }
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
