package de.drivechronik.dash

import android.location.Location

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
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.layers.PropertyFactory.*
import org.maplibre.android.style.expressions.Expression
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.Point
import org.maplibre.geojson.LineString
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.CircleLayer
import de.drivechronik.dash.routing.RoutePoint
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView

@Composable
internal fun NavigationMap(
    modifier: Modifier = Modifier,
    routePoints: List<RoutePoint> = emptyList(),
    phoneState: State<Location?>
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val compassHeading = rememberCompassHeading()
    val currentCompass by rememberUpdatedState(compassHeading)
    val currentRoutePoints by rememberUpdatedState(routePoints)
    // Gemeinsames Navigations-GPS
    val mapState = remember {
        mutableStateOf<MapLibreMap?>(null)
    }
    var lastBearing by remember {
        mutableFloatStateOf(0f)
    }
    var lastDriveBearing by remember {
        mutableFloatStateOf(0f)
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
                    it.addSource(
                        GeoJsonSource(
                            "dash-route",
                            FeatureCollection.fromFeatures(
                                emptyArray<Feature>()
                            )
                        )
                    )

                    it.addLayer(
                        LineLayer(
                            "dash-route-layer", "dash-route"
                        ).withProperties(
                            lineColor("#168BFF"),
                            lineWidth(5f),
                            lineOpacity(0.95f)
                        )
                    )

                    listOf(
                        Triple("dash-start", "#20B66A", 8f),
                        Triple("dash-end", "#EA4D50", 9f)
                    ).forEach { (id, color, radius) ->
                        it.addSource(
                            GeoJsonSource(
                                id,
                                FeatureCollection.fromFeatures(
                                    emptyArray<Feature>()
                                )
                            )
                        )
                        it.addLayer(
                            CircleLayer("$id-layer", id)
                                .withProperties(
                                    circleColor(color),
                                    circleRadius(radius),
                                    circleStrokeColor("#FFFFFF"),
                                    circleStrokeWidth(2.5f)
                                )
                        )
                    }

                    it.addImage(
                        "dash-arrow", navigationArrowBitmap()
                    )
                    it.addSource(
                        GeoJsonSource(
                            "dash-position",
                            FeatureCollection.fromFeatures(
                                emptyArray<Feature>()
                            )
                        )
                    )
                    it.addLayer(
                        SymbolLayer(
                            "dash-arrow-layer", "dash-position"
                        ).withProperties(
                            iconImage("dash-arrow"),
                            iconSize(0.55f),
                            iconRotate(Expression.get("bearing")),
                            iconRotationAlignment("map"),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                    )
                    mapState.value = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(LatLng(51.16, 10.45))
                        .zoom(5.0)
                        .build()
                }
            }
        }
    }

    LaunchedEffect(routePoints, mapState.value) {
        mapState.value?.style
            ?.getSourceAs<GeoJsonSource>("dash-route")
            ?.setGeoJson(
                if (routePoints.size >= 2) {
                    FeatureCollection.fromFeature(
                        Feature.fromGeometry(
                            LineString.fromLngLats(
                                routePoints.map {
                                    Point.fromLngLat(it.lon, it.lat)
                                }
                            )
                        )
                    )
                } else {
                    FeatureCollection.fromFeatures(
                        emptyArray<Feature>()
                    )
                }
            )
    }

    LaunchedEffect(routePoints, mapState.value) {
        val map = mapState.value
        if (routePoints.size < 2) {
            centered = true
            val fix = currentNavigationFix(phoneState.value)
            if (map != null && fix != null) {
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(fix.point.lat, fix.point.lon),
                        15.5
                    )
                )
            }
            return@LaunchedEffect
        }

        if (map != null && routePoints.size >= 2) {
            // Beim Rerouting Kamera weiter folgen lassen.
            if (NavigationSimulation.active && centered) {
                return@LaunchedEffect
            }
            centered = false
            val bounds = LatLngBounds.Builder().apply {
                routePoints.forEach {
                    include(LatLng(it.lat, it.lon))
                }
            }.build()
            map.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 60)
            )
            delay(4_000L)
            centered = true
        }
    }

    LaunchedEffect(routePoints, mapState.value) {
        val style = mapState.value?.style
        listOf(
            "dash-start" to routePoints.firstOrNull(),
            "dash-end" to routePoints.lastOrNull()
        ).forEach { (id, point) ->
            val source = style?.getSourceAs<GeoJsonSource>(id)
            if (point == null) {
                source?.setGeoJson(
                    FeatureCollection.fromFeatures(
                        emptyArray<Feature>()
                    )
                )
            } else {
                source?.setGeoJson(
                    Feature.fromGeometry(
                        Point.fromLngLat(point.lon, point.lat)
                    )
                )
            }
        }
    }

    LaunchedEffect(mapView) {
        var nearTurn = false
        var lastFrame = 0L
        var lastCameraUpdate = 0L
        while (true) {
            val gps = if (NavigationSimulation.active) {
                        NavigationSimulation.fix
                            ?.takeUnless {
                                NavigationSimulation.gpsOutage
                            }?.let {
                            it.point.lat to it.point.lon
                        }
                    } else {
                        weatherCoordinates(phoneState.value)
                    }
            val map = mapState.value

            if (map != null) {
                map.style?.getSourceAs<GeoJsonSource>(
                    "dash-position"
                )?.let { source ->
                    if (gps == null) {
                        source.setGeoJson(
                            FeatureCollection.fromFeatures(
                                emptyArray<Feature>()
                            )
                        )
                    } else {
                        val bearing = (
                            if (NavigationSimulation.active)
                                NavigationSimulation.fix?.heading
                            else navigationBearing(
                                phoneState.value, gps
                            )
                        ) ?: currentCompass
                        if (bearing != null) {
                            lastBearing = bearing
                        }
                        (if (NavigationSimulation.active)
                            NavigationSimulation.fix?.heading
                        else navigationBearing(
                            phoneState.value, gps
                        ))?.let {
                            lastDriveBearing = it
                        }
                        val feature = Feature.fromGeometry(
                            Point.fromLngLat(
                                gps.second, gps.first
                            )
                        )
                        feature.addNumberProperty(
                            "bearing", lastBearing
                        )
                        source.setGeoJson(feature)
                    }
                }
            }

            val liveFix = currentNavigationFix(phoneState.value)
            val cameraGps = liveFix?.let {
                it.point.lat to it.point.lon
            } ?: if (NavigationSimulation.active) {
                null
            } else {
                weatherCoordinates(phoneState.value)
            }

            if (centered && cameraGps != null && map != null) {

                val hint = NavigationHintState.hint
                val distance = hint
                    ?.takeUnless { it.stale }
                    ?.distance

                if (hint == null) {
                    nearTurn = false
                } else if (distance != null) {
                    if (distance <= 300.0) nearTurn = true
                    if (distance >= 340.0) nearTurn = false
                }

                val targetZoom =
                    if (nearTurn) 16.0 else 13.5


                val camera = map.cameraPosition
                val now = android.os.SystemClock.elapsedRealtime()
                val dt = if (lastFrame == 0L) 50.0
                    else (now - lastFrame)
                        .coerceIn(0L, 100L).toDouble()
                lastFrame = now

                fun blend(ms: Double): Double =
                    1.0 - kotlin.math.exp(-dt / ms)

                // Kontinuierlicher Zoom, ca. 1,7 Sekunden.
                val zoom = camera.zoom +
                    (targetZoom - camera.zoom) * blend(550.0)

                // Position ebenfalls weich nachführen.
                val target = camera.target
                    ?: LatLng(cameraGps.first, cameraGps.second)
                val lat = target.latitude +
                    (cameraGps.first - target.latitude) *
                    blend(220.0)
                val lon = target.longitude +
                    (cameraGps.second - target.longitude) *
                    blend(220.0)

                val direction =
                    if (liveFix?.source == "simulator")
                        liveFix.heading
                    else if (liveFix != null)
                        navigationBearing(
                            phoneState.value, cameraGps
                        )
                    else null

                val desired = direction?.toDouble()
                    ?: camera.bearing
                var delta = (desired - camera.bearing +
                    540.0) % 360.0 - 180.0

                if (NavigationSimulation.active &&
                    kotlin.math.abs(delta) < 12.0) {
                    delta = 0.0
                }

                val bearing = camera.bearing +
                    delta * blend(300.0)

                if (kotlin.math.abs(
                        zoom - camera.zoom
                    ) > 0.001 ||
                    kotlin.math.abs(
                        lat - target.latitude
                    ) > 0.000001 ||
                    kotlin.math.abs(
                        lon - target.longitude
                    ) > 0.000001 ||
                    kotlin.math.abs(
                        bearing - camera.bearing
                    ) > 0.05) {

                    map.moveCamera(
                        CameraUpdateFactory.newCameraPosition(
                            CameraPosition.Builder()
                                .target(LatLng(lat, lon))
                                .zoom(zoom)
                                .bearing(bearing)
                                .build()
                        )
                    )
                }

            }

            delay(50L)
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
                    val gps = if (NavigationSimulation.active) {
                        NavigationSimulation.fix
                            ?.takeUnless {
                                NavigationSimulation.gpsOutage
                            }?.let {
                            it.point.lat to it.point.lon
                        }
                    } else {
                        weatherCoordinates(phoneState.value)
                    }
                    val map = mapState.value
                    if (map != null) {
                        if (centered) {
                            centered = false
                            if (routePoints.size >= 2) {
                                val bounds = LatLngBounds.Builder()
                                    .apply {
                                        routePoints.forEach {
                                            include(LatLng(it.lat, it.lon))
                                        }
                                    }.build()
                                map.animateCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        bounds, 60
                                    )
                                )
                            }
                        } else {
                            centered = true
                        }
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
