package de.drivechronik.dash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
internal fun NavigationSettingsHome(
    onDeveloperMode: () -> Unit
) {
    val context = LocalContext.current
    var mapsOpen by remember { mutableStateOf(false) }
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
