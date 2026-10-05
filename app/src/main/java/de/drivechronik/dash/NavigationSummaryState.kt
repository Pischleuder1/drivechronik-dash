package de.drivechronik.dash

import androidx.compose.runtime.*

internal object NavigationSummaryState {
    var text by mutableStateOf<String?>(null)
        private set

    fun update(value: String) {
        if (text != value) text = value
    }

    fun clear() {
        text = null
    }
}
