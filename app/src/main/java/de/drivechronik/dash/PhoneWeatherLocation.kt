package de.drivechronik.dash

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

@Composable
internal fun rememberPhoneWeatherLocation(
    navigation: Boolean = false
):
    Triple<State<Location?>, Boolean, () -> Unit> {

    val context = LocalContext.current
    val position = remember { mutableStateOf<Location?>(null) }

    fun permitted(): Boolean =
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).any {
            ContextCompat.checkSelfPermission(context, it) ==
                PackageManager.PERMISSION_GRANTED
        }

    var allowed by remember { mutableStateOf(permitted()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        allowed = permitted()
    }

    DisposableEffect(allowed, navigation) {
        val manager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val previous = position.value
                if (previous == null ||
                    location.elapsedRealtimeNanos >
                    previous.elapsedRealtimeNanos
                ) position.value = location
            }
        }

        if (allowed) {
            for (provider in listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER
            )) {
                try {
                    if (!manager.isProviderEnabled(provider)) continue

                    manager.getLastKnownLocation(provider)?.let {
                        listener.onLocationChanged(it)
                    }

                    manager.requestLocationUpdates(
                        provider,
                        if (navigation) 1_000L else 15_000L,
                        if (navigation) 0f else 20f,
                        listener, Looper.getMainLooper()
                    )
                } catch (_: SecurityException) {
                } catch (_: IllegalArgumentException) {
                }
            }
        }

        onDispose {
            manager.removeUpdates(listener)
        }
    }

    return Triple(position, allowed) {
        launcher.launch(arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ))
    }
}
