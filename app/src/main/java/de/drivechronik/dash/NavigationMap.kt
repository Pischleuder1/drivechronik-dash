package de.drivechronik.dash

import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.annotations.Marker
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

@Composable
internal fun NavigationMap(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val (phoneState, _, _) = rememberPhoneWeatherLocation()
    val mapState = remember {
        mutableStateOf<MapLibreMap?>(null)
    }
    var locationMarker by remember {
        mutableStateOf<Marker?>(null)
    }
    var centered by remember {
        mutableStateOf(false)
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(
            context,
            MapLibreMapOptions().textureMode(true)
        ).apply {
            onCreate(null)
            getMapAsync { map ->
                map.setStyle(
                    "https://tiles.openfreemap.org/styles/liberty"
                ) {
                    mapState.value = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(51.16, 10.45))
                        .zoom(5.0)
                        .build()
                }
            }
        }
    }

    LaunchedEffect(mapView) {
        while (true) {
            val gps = weatherCoordinates(phoneState.value)
            val map = mapState.value

            if (gps != null && map != null) {
                val position = LatLng(gps.first, gps.second)
                val marker = locationMarker
                if (marker == null) {
                    locationMarker = map.addMarker(
                        MarkerOptions().position(position)
                    )
                } else {
                    marker.position = position
                    map.updateMarker(marker)
                }
            }

            if (!centered && gps != null && map != null) {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(gps.first, gps.second),
                        15.0
                    )
                )
                centered = true
            }

            delay(3_000L)
        }
    }

    DisposableEffect(mapView, owner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(observer)
        onDispose {
            owner.lifecycle.removeObserver(observer)
            if (owner.lifecycle.currentState.isAtLeast(
                    Lifecycle.State.RESUMED
                )) mapView.onPause()
            if (owner.lifecycle.currentState.isAtLeast(
                    Lifecycle.State.STARTED
                )) mapView.onStop()
            mapView.onDestroy()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize()
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(0xFF172B40))
                .clickable {
                    val gps = weatherCoordinates(phoneState.value)
                    val map = mapState.value
                    if (gps != null && map != null) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(
                                LatLng(gps.first, gps.second),
                                15.0
                            )
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "◎",
                color = Color(0xFF70C8FF),
                fontSize = 27.sp
            )
        }
    }
}
