package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
internal fun DashUpdateSection(currentVersion: String) {

    var checking by remember { mutableStateOf(false) }
    var status by remember {
        mutableStateOf("Noch nicht geprüft")
    }
    var update by remember {
        mutableStateOf<DashUpdateInfo?>(null)
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var downloadedApk by remember {
        mutableStateOf<File?>(null)
    }
    var installPermissionPending by remember {
        mutableStateOf(false)
    }

    fun beginInstallation(apk: File) {
        if (DashUpdateInstaller.canInstall(context)) {
            installPermissionPending = false
            DashUpdateInstaller.install(context, apk)
        } else {
            installPermissionPending = true
            DashUpdateInstaller.openInstallPermission(context)
        }
    }

    DisposableEffect(lifecycleOwner, installPermissionPending) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME &&
                installPermissionPending &&
                DashUpdateInstaller.canInstall(context)
            ) {
                downloadedApk?.let { apk ->
                    installPermissionPending = false
                    DashUpdateInstaller.install(context, apk)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    suspend fun checkForUpdates() {
        if (checking) return

        checking = true
        status = "GitHub wird geprüft ..."

        try {
            DashUpdateChecker.check(currentVersion)
                .fold(
                    onSuccess = { available ->
                        update = available
                        status = if (available == null) {
                            "Die App ist auf dem neuesten Stand."
                        } else {
                            "Neue Version v${available.version} verfügbar"
                        }
                    },
                    onFailure = { error ->
                        update = null
                        status = "Update-Prüfung fehlgeschlagen: " +
                            (error.message ?: "Unbekannter Fehler")
                    }
                )
        } finally {
            checking = false
        }
    }

    LaunchedEffect(currentVersion) {
        checkForUpdates()
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        NavigationSettingsSectionHeader(title = "APP-UPDATES")

        Text(
            text = "Installierte Version: v$currentVersion",
            modifier = Modifier.padding(start = 8.dp, top = 4.dp),
            style = MaterialTheme.typography.bodyMedium
        )

        Text(
            text = status,
            modifier = Modifier.padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall
        )

        update?.let { available ->
            if (available.releaseNotes.isNotBlank()) {
                Text(
                    available.releaseNotes.take(600),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (downloading) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Download: $progress %")
            }

            Button(
                onClick = {
                    scope.launch {
                        downloading = true
                        progress = 0
                        status = "APK wird heruntergeladen ..."

                        try {
                            DashUpdateDownloader.download(
                                context,
                                available
                            ) { percent ->
                                scope.launch(Dispatchers.Main) {
                                    progress = percent
                                }
                            }.fold(
                                onSuccess = { apk ->
                                    downloadedApk = apk
                                    status = "APK geprüft. Installation starten."
                                    beginInstallation(apk)
                                },
                                onFailure = { error ->
                                    status = "Download fehlgeschlagen: " +
                                        (error.message ?: "Unbekannter Fehler")
                                }
                            )
                        } catch (e: Exception) {
                            status = "Update fehlgeschlagen: " +
                                (e.message ?: "Unbekannter Fehler")
                        } finally {
                            downloading = false
                        }
                    }
                },
                enabled = !downloading && !checking,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    if (downloading) "Download läuft ..."
                    else "Update herunterladen und installieren"
                )
            }

            if (downloadedApk != null && !downloading) {
                OutlinedButton(
                    onClick = {
                        downloadedApk?.let {
                            beginInstallation(it)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Installation erneut starten")
                }
            }
        }

        OutlinedButton(
            onClick = {
                scope.launch {
                    checkForUpdates()
                }
            },
            enabled = !checking,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (checking) "Prüfung läuft ..."
                else "Auf Updates prüfen"
            )
        }
    }
}
