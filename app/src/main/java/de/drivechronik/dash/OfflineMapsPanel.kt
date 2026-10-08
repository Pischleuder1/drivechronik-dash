package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.content.Intent
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
internal fun OfflineMapsPanel(onBack: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var remote by remember {
        mutableStateOf<List<RemoteMapPackage>>(emptyList())
    }
    var error by remember { mutableStateOf<String?>(null) }
    var installedStatus by remember {
        mutableStateOf<Map<String, MapInstallStatus>>(emptyMap())
    }
    var checking by remember { mutableStateOf(false) }
    val downloadScope = rememberCoroutineScope()
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var downloadInfo by remember { mutableStateOf("") }
    var restartCountdown by remember { mutableStateOf<Int?>(null) }

    var downloadResults by remember {
        mutableStateOf<Map<String, String>>(emptyMap())
    }


    LaunchedEffect(remote) {
        installedStatus = emptyMap()
        checking = remote.isNotEmpty()
        try {
            for (pkg in remote) {
                val status = OfflineMapStatus.check(context, pkg)
                installedStatus = installedStatus + (pkg.id to status)
            }
        } finally {
            checking = false
        }
    }


    LaunchedEffect(refresh) {
        loading = true
        GitHubMapCatalog.fetch(context)
            .onSuccess {
                remote = it
                error = null
            }
            .onFailure {
                error = it.message ?: "GitHub nicht erreichbar"
            }
        loading = false
    }

    val map = context.getExternalFilesDir(null)
        ?.resolve("germany.pmtiles")
    val routing = File(
        context.filesDir, "routing/valhalla_tiles.tar"
    )
    val marker = context.getExternalFilesDir(null)
        ?.resolve("offline-map.enabled")
    val testApp = context.packageName.endsWith(".routingtest")
    val mapEnabled = testApp &&
        map?.isFile == true && marker?.isFile == true

    fun size(bytes: Long): String =
        String.format(
            Locale.GERMANY, "%.2f GB",
            bytes / 1_073_741_824.0
        )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Offline-Karten", style = MaterialTheme.typography.titleLarge)
        Text(
            "Alle Offline-Pakete sind optional. " +
            "Für die Tesla-Anzeige ist kein Download nötig."
        )

        HorizontalDivider()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapInfoIcon("wifi")
            Text("ONLINE-KARTEN",
                style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "Kartendarstellung über Internet. " +
            "Online-Routing wird später ergänzt."
        )

        HorizontalDivider()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapInfoIcon("map")
            Text("OFFLINE-KARTEN · PMTiles",
                style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "Zeigt Straßen und Orte ohne Internet. " +
            "Enthält keine Routenberechnung."
        )

        HorizontalDivider()
        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MapInfoIcon("route")
            Text("OFFLINE-ROUTING · Valhalla",
                style = MaterialTheme.typography.titleSmall)
        }
        Text(
            "WICHTIG: Für die vollständige Navigation ohne Internet " +
            "müssen für das jeweilige Land BEIDE Komponenten " +
            "installiert sein:\n" +
            "1. Offline-Karte (PMTiles): Kartendarstellung.\n" +
            "2. Offline-Routing (Valhalla): Routenberechnung " +
            "und Abbiegehinweise.\n" +
            "Nur zusammen ermöglichen sie die vollständige " +
            "Offline-Navigation."
        )

        HorizontalDivider()

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            GitHubMapIcon()
            Text(
                "GITHUB · VERFÜGBARE DOWNLOADS",
                style = MaterialTheme.typography.titleMedium
            )
        }

        TextButton(onClick = { refresh++ }) {
            Text("Aktualisieren")
        }

        if (checking) {
            Text("Prüfe installierte Dateien (SHA-256) …")
        }

        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        } else {
            if (error != null) {
                Text("GitHub-Abfrage: $error")
            }
            if (remote.isEmpty()) {
                Text("Derzeit keine Downloadpakete veröffentlicht.")
            }

            remote.groupBy { it.country }.forEach { (_, items) ->
                HorizontalDivider()
                Text(
                    "${mapCountryFlag(items.first().country)}  " +
                        items.first().countryName,
                    style = MaterialTheme.typography.titleMedium
                )
                items.forEach { item ->
                    val status = installedStatus[item.id]
                    val type = when (item.kind) {
                        "map" -> "PMTiles"
                        "routing" -> "Valhalla"
                        "charging" -> "Ladestationen"
                        else -> item.kind
                    }
                    val state = when (status) {
                        MapInstallStatus.CURRENT -> "✓ Aktuell"
                        MapInstallStatus.UPDATE_AVAILABLE ->
                            "↑ Update verfügbar"
                        MapInstallStatus.NOT_INSTALLED ->
                            "Nicht installiert"
                        MapInstallStatus.UNKNOWN_VERSION ->
                            "Version unbekannt"
                        null -> "Prüfung …"
                    }


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.SpaceBetween,
                        verticalAlignment =
                            androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            "$type · ${size(item.sizeBytes)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            state,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (status != MapInstallStatus.CURRENT) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                        TextButton(
                            enabled = downloadingId == null,
                            onClick = {
                                downloadingId = item.id
                                downloadResults =
                                    downloadResults - item.id
                                downloadScope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            OfflineMapStorage.check(context, item)
                                            item.parts.forEachIndexed { i, part ->
                                                val previous = item.parts
                                                    .take(i).sumOf { it.sizeBytes }
                                                var lastPercent = -1L

                                                OfflineMapDownloader.downloadPart(
                                                    context, item, part
                                                ) { done, _ ->
                                                    val completed = previous + done
                                                    val percent = if (item.sizeBytes > 0)
                                                        completed * 100 / item.sizeBytes
                                                    else 0L

                                                    if (percent != lastPercent) {
                                                        lastPercent = percent
                                                        downloadScope.launch(Dispatchers.Main) {
                                                            downloadInfo =
                                                                "${size(completed)} / " +
                                                                "${size(item.sizeBytes)} " +
                                                                "($percent %)"
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        downloadInfo = "Prüfe Gesamtpaket …"

                                        withContext(Dispatchers.IO) {
                                            val verified =
                                                OfflineMapAssembler.assemble(context, item)

                                            if (OfflinePackageSupport.isSupported(item.id)) {
                                                OfflineMapPending.mark(
                                                    context, item, verified
                                                )
                                            }
                                        }

                                        downloadInfo = "Installiere …"

                                        val installed =
                                            withContext(Dispatchers.IO) {
                                                if (OfflinePackageSupport.isSupported(item.id)) {
                                                    OfflinePendingInstaller.process(
                                                        context, item.id
                                                    )
                                                } else {
                                                    false
                                                }
                                            }

                                        check(installed) {
                                            "Installation wurde nicht durchgeführt"
                                        }

                                        installedStatus =
                                            installedStatus +
                                                (item.id to MapInstallStatus.CURRENT)

                                        downloadInfo = "Installiert"

                                        for (seconds in 5 downTo 1) {
                                            restartCountdown = seconds
                                            downloadResults =
                                                downloadResults +
                                                    (
                                                        item.id to
                                                            "✓ Installiert · " +
                                                            "Neustart in " +
                                                            "$seconds Sekunden, " +
                                                            "um Änderungen " +
                                                            "zu übernehmen"
                                                    )
                                            delay(1000L)
                                        }

                                        restartCountdown = null

                                        val launchIntent =
                                            context.packageManager
                                                .getLaunchIntentForPackage(
                                                    context.packageName
                                                )
                                                ?: error(
                                                    "Start-Intent fehlt"
                                                )

                                        launchIntent.addFlags(
                                            Intent.FLAG_ACTIVITY_NEW_TASK or
                                                Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        )

                                        (context as? Activity)
                                            ?.finishAffinity()

                                        context.startActivity(launchIntent)

                                        return@launch
                                    } catch (e: Exception) {
                                        downloadInfo =
                                            "Fehler: ${e.message ?: "Download"}"
                                        downloadResults = downloadResults +
                                            (item.id to downloadInfo)
                                    } finally {
                                        downloadingId = null
                                    }
                                }
                            }
                        ) {
                            val actionText = when (status) {
                                MapInstallStatus.UPDATE_AVAILABLE ->
                                    "Aktualisieren"
                                MapInstallStatus.NOT_INSTALLED ->
                                    "Herunterladen"
                                MapInstallStatus.UNKNOWN_VERSION ->
                                    "Neu installieren"
                                else ->
                                    "Herunterladen"
                            }

                            Text(
                                if (downloadingId == item.id)
                                    "Lädt: $downloadInfo"
                                else
                                    downloadResults[item.id]
                                        ?: actionText
                            )
                        }
                        }
                    }

                }
            }
        }

        val storage = android.os.StatFs(context.filesDir.path)
        Text("Freier Speicher: ${size(storage.availableBytes)}")
        Text(
            "Offline-Pakete können hier heruntergeladen und " +
            "aktualisiert werden.",
            style = MaterialTheme.typography.bodySmall
        )

        TextButton(onClick = onBack) {
            Text("Zurück zu Einstellungen")
        }
    }
}
