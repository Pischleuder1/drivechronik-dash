package de.drivechronik.dash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun NavigationSettingsHome(
    route: de.drivechronik.dash.routing.OfflineRoute?,
    onStop: () -> Unit,
    close: () -> Unit,
    onDeveloperMode: () -> Unit,
    initialMapsOpen: Boolean = false
) {
    val context = LocalContext.current
    var mapsOpen by remember(initialMapsOpen) {
        mutableStateOf(initialMapsOpen)
    }
    var taps by remember { mutableIntStateOf(0) }
    val version = remember {
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName ?: "?"
    }

    if (mapsOpen) {
        OfflineMapsPanel(
            onBack = { mapsOpen = false }
        )
        return
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text("KARTEN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium)

        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color(0xFF21364B)
            ),
            onClick = { mapsOpen = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color(0xFF21364B)
                ),
                headlineContent = { Text("Offline-Karten") },
                supportingContent = {
                    Text("Länderkarten und Speicher verwalten")
                },
                leadingContent = { Text("▣") },
                trailingContent = { Text("›") }
            )
        }

        Text("ROUTENOPTIONEN",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium)

        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color(0xFF21364B)
            ),modifier = Modifier.fillMaxWidth()) {
            ListItem(
                colors = ListItemDefaults.colors(
                    containerColor = Color(0xFF21364B)
                ),
                headlineContent = { Text("Autobahn vermeiden") },
                supportingContent = {
                    Text("Über das Autobahnschild schalten")
                },
                leadingContent = { Text("⇢") }
            )
        }

        Text(
            "FAHRTSIMULATION",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelMedium
        )

        val sim = NavigationSimulation

        OutlinedCard(
            colors = CardDefaults.outlinedCardColors(
                containerColor = Color(0xFF21364B)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (!sim.active) {
                Button(
                    onClick = {
                        route?.let { sim.start(it) }
                        close()
                    },
                    enabled = route != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .height(36.dp),
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 0.dp
                    )
                ) {
                    Text("SIM START", fontSize = 12.sp)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement =
                        Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { sim.togglePause() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 6.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            if (sim.paused) "WEITER" else "PAUSE",
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = { sim.changeSpeed() },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 6.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text(
                            "${sim.speedKmh} km/h",
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            onStop()
                            close()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp),
                        contentPadding = PaddingValues(
                            horizontal = 6.dp,
                            vertical = 0.dp
                        )
                    ) {
                        Text("STOPP", fontSize = 11.sp)
                    }
                }
            }
        }

        HorizontalDivider()
        Text(
            text = "Version $version",
            modifier = Modifier.clickable {
                taps++
                if (taps >= 5) {
                    taps = 0
                    onDeveloperMode()
                }
            }
        )
    }
}
