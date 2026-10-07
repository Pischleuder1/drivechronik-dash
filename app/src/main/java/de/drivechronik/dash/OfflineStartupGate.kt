package de.drivechronik.dash

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object OfflineStartupGate {
    var mapReady by mutableStateOf(false)
        private set
    var routingReady by mutableStateOf(false)
        private set

    var mapError by mutableStateOf<String?>(null)
        private set
    var routingError by mutableStateOf<String?>(null)
        private set

    val ready get() = mapReady && routingReady
    val error get() = mapError ?: routingError

    fun begin() {
        mapReady = false
        routingReady = false
        mapError = null
        routingError = null
    }

    fun mapSuccess() {
        mapError = null
        mapReady = true
    }

    fun routingSuccess() {
        routingError = null
        routingReady = true
    }

    fun mapFailure(message: String) {
        mapReady = false
        mapError = message
    }

    fun routingFailure(message: String) {
        routingReady = false
        routingError = message
    }

    fun success() {
        mapSuccess()
        routingSuccess()
    }

    fun failure(message: String) {
        mapFailure(message)
        routingFailure(message)
    }
}
