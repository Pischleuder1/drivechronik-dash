package de.drivechronik.dash

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Mic
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.*
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat

@Composable
internal fun DriveChronikWakeControl(
    suspended: Boolean,
    onWake: () -> Unit
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val currentWake by rememberUpdatedState(onWake)

    var enabled by rememberSaveable { mutableStateOf(false) }
    var status by remember { mutableStateOf("Aus") }
    var resumed by remember {
        mutableStateOf(
            owner.lifecycle.currentState.isAtLeast(
                Lifecycle.State.RESUMED
            )
        )
    }

    val wake = remember(context) {
        DriveChronikWakeWord(
            context,
            { currentWake() },
            { status = it }
        )
    }

    val permission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        enabled = granted
        if (!granted) status = "Mikrofon nicht erlaubt"
    }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(wake, enabled, resumed, suspended) {
        if (enabled && resumed && !suspended) {
            wake.start()
        } else {
            wake.stop()
        }
        onDispose { wake.stop() }
    }

    IconButton(
        modifier = Modifier.size(44.dp).background(
            if (enabled) Color(0xFF168B60)
            else Color(0xFF192B3C),
            RoundedCornerShape(10.dp)
        ),
        onClick = {
            if (enabled) {
                enabled = false
                status = "Aus"
            } else if (
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                enabled = true
            } else {
                permission.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }
        }
    ) {
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Mic,
            contentDescription = if (enabled)
                "Sprachsteuerung ausschalten"
            else "Sprachsteuerung einschalten",
            tint = Color.White
        )
    }


}
