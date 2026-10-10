package de.drivechronik.dash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal enum class TrafficEventType {
    ROADWORK,
    WARNING,
    CLOSURE
}

internal data class TrafficEvent(
    val id: String,
    val type: TrafficEventType,
    val road: String,
    val title: String,
    val subtitle: String = "",
    val lat: Double,
    val lon: Double,
    val blocked: Boolean = false,
    val geometry: List<de.drivechronik.dash.routing.RoutePoint> =
        emptyList()
)

internal object TrafficEventState {
    var events by mutableStateOf<List<TrafficEvent>>(emptyList())
        private set

    var lastUpdatedAt by mutableStateOf<Long?>(null)
        private set

    var replayGeneration by mutableStateOf(0)
        private set

    fun requestReplay() {
        if (events.isNotEmpty()) {
            replayGeneration++
        }
    }

    fun update(value: List<TrafficEvent>) {
        events = value
        lastUpdatedAt = System.currentTimeMillis()
    }

    fun clear() {
        events = emptyList()
        lastUpdatedAt = null
    }
}
