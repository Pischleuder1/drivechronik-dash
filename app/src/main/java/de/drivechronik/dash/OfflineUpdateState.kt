package de.drivechronik.dash

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.setValue

internal object OfflineUpdateState {

    var updateCount by mutableIntStateOf(0)
        private set

    var attentionCount by mutableIntStateOf(0)
        private set

    var openOfflineMapsRequest by mutableLongStateOf(0L)
        private set

    suspend fun refresh(context: Context) {
        val packages = GitHubMapCatalog.fetch(context)
            .getOrNull()
            ?: return

        var updates = 0
        var attention = 0

        packages
            .filter {
                OfflinePackageSupport.isSupported(it.id)
            }
            .forEach { pkg ->
                val file = runCatching {
                    OfflinePackageSupport.target(
                        context,
                        pkg
                    )
                }.getOrNull()
                    ?: return@forEach

                // Nicht installierte optionale Pakete erzeugen
                // bewusst keinen Dashboard-Hinweis.
                if (!file.isFile) {
                    return@forEach
                }

                val installedVersion =
                    OfflineMapVersions.version(
                        context,
                        pkg.id
                    )

                val installedHash =
                    OfflineMapVersions.hash(
                        context,
                        pkg.id
                    )

                when {
                    installedVersion == null ||
                        installedHash == null -> {
                        attention++
                    }

                    !installedHash.equals(
                        pkg.sha256,
                        ignoreCase = true
                    ) -> {
                        updates++
                    }

                    installedVersion != pkg.version -> {
                        updates++
                    }
                }
            }

        updateCount = updates
        attentionCount = attention
    }

    fun requestOpenOfflineMaps() {
        openOfflineMapsRequest++
    }
}
