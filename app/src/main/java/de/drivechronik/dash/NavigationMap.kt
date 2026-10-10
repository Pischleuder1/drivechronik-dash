package de.drivechronik.dash

import androidx.compose.foundation.layout.fillMaxHeight

import androidx.compose.foundation.layout.fillMaxWidth

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
import org.maplibre.android.style.sources.VectorSource
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
import org.maplibre.android.maps.Style

@Composable
internal fun NavigationMap(
    nightMode: Boolean = false,
    modifier: Modifier = Modifier,
    routePoints: List<RoutePoint> = emptyList(),
    routeGeneration: Int = 0,
    phoneState: State<Location?>,
    onChargingStationClick: (ChargingStationSelection) -> Unit = {}
) {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current

    val activeCountry =
        OfflineMapRepository.activeCountry(context)
    val compassHeading = rememberCompassHeading()
    val currentCompass by rememberUpdatedState(compassHeading)
    val currentRoutePoints by rememberUpdatedState(routePoints)
    val currentChargingStationClick by rememberUpdatedState(
        onChargingStationClick
    )
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

    var lastOverviewGeneration by remember {
        mutableStateOf<Int?>(null)
    }

    val mapView = remember(
        context,
        activeCountry,
        nightMode
    ) {
        MapLibre.getInstance(context.applicationContext)
        MapView(
            context,
            MapLibreMapOptions().textureMode(true)
        ).apply {
            onCreate(null)
            getMapAsync { map ->
                val offlineDir =
                    context.getExternalFilesDir(null)

                val activeOfflinePackage =
                    OfflineMapRepository.packageFor(
                        context,
                        activeCountry
                    )

                val offlineFile =
                    activeOfflinePackage.mapFile

                val chargingFile =
                    activeOfflinePackage.chargingFile

                val offlineEnabled =
                    context.packageName.endsWith(".routingtest") &&
                    offlineFile?.isFile == true &&
                    offlineDir
                        ?.resolve("offline-map.enabled")
                        ?.isFile == true

                val chargingUri =
                    if (chargingFile?.isFile == true) {
                        "pmtiles://file://" +
                            chargingFile.absolutePath
                    } else {
                        null
                    }

                val styleBuilder = if (offlineEnabled) {
                    val template = context.assets.open(
                        "maps/offline-test.json"
                    ).bufferedReader().use { it.readText() }

                    val uri = "pmtiles://file://" +
                        offlineFile!!.absolutePath

                    android.util.Log.i(
                        "DriveChronikOfflineMap",
                        "Offline ${activeOfflinePackage.name}: $uri"
                    )

                    if (chargingUri != null) {
                        android.util.Log.i(
                            "DriveChronikOfflineMap",
                            "Charging-Overlay: $chargingUri"
                        )
                    }

                    val chargingSourceBlock =
                        if (chargingUri != null) {
                            """,
    "charging": {
      "type": "vector",
      "url": "$chargingUri",
      "attribution": "Ladeinfrastruktur: Bundesnetzagentur.de"
    }"""
                        } else {
                            ""
                        }

                    val styleJson = template
                        .replace(
                            "PMTILES_FILE_URL",
                            uri
                        )
                        .replace(
                            "CHARGING_SOURCE_BLOCK",
                            chargingSourceBlock
                        )

                    Style.Builder().fromJson(styleJson)
                } else {
                    Style.Builder().fromUri(
                        if (nightMode) {
                            "https://tiles.openfreemap.org/styles/dark"
                        } else {
                            "https://tiles.openfreemap.org/styles/liberty"
                        }
                    )
                }

                map.setStyle(styleBuilder) {
                    /*
                     * Ladeinfrastruktur ist unabhängig von der
                     * verwendeten Basiskarte.
                     *
                     * Beim Offline-Style ist die Source bereits
                     * im Style-JSON enthalten. Beim Online-Style
                     * wird sie hier nachträglich ergänzt.
                     */
                    if (
                        chargingUri != null &&
                        it.getSource("charging") == null
                    ) {
                        it.addSource(
                            VectorSource(
                                "charging",
                                chargingUri
                            )
                        )
                    }

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

                    if (chargingUri != null) {
                        it.addImage(
                            "dash-charging-normal",
                            chargingStationBitmap()
                        )
                        it.addImage(
                            "dash-charging-fast",
                            fastChargingBitmap()
                        )

                        // Normale Ladepunkte – erst bei näherem Zoom.
                        val normalChargingLayer = SymbolLayer(
                            "charging-normal-layer",
                            "charging"
                        ).withSourceLayer(
                            "charging_station"
                        ).withFilter(
                            Expression.all(
                                Expression.eq(
                                    Expression.get("is_fast"),
                                    Expression.literal(false)
                                ),
                                Expression.eq(
                                    Expression.get("is_supercharger"),
                                    Expression.literal(false)
                                )
                            )
                        ).withProperties(
                            iconImage("dash-charging-normal"),
                            iconSize(0.72f),
                            iconAllowOverlap(false),
                            iconIgnorePlacement(false)
                        )
                        normalChargingLayer.minZoom = 12f
                        it.addLayer(normalChargingLayer)

                        // Andere Schnelllader.
                        val fastChargingLayer = SymbolLayer(
                            "charging-fast-layer",
                            "charging"
                        ).withSourceLayer(
                            "charging_station"
                        ).withFilter(
                            Expression.all(
                                Expression.eq(
                                    Expression.get("is_fast"),
                                    Expression.literal(true)
                                ),
                                Expression.eq(
                                    Expression.get("is_supercharger"),
                                    Expression.literal(false)
                                )
                            )
                        ).withProperties(
                            iconImage("dash-charging-fast"),
                            iconSize(0.78f),
                            iconAllowOverlap(false),
                            iconIgnorePlacement(false)
                        )
                        fastChargingLayer.minZoom = 10f
                        it.addLayer(fastChargingLayer)

                        // Tesla Supercharger.
                        it.addImage(
                            "dash-tesla-supercharger",
                            teslaSuperchargerBitmap()
                        )

                        val teslaChargingLayer = SymbolLayer(
                            "charging-tesla-layer",
                            "charging"
                        ).withSourceLayer(
                            "charging_station"
                        ).withFilter(
                            Expression.eq(
                                Expression.get("is_supercharger"),
                                Expression.literal(true)
                            )
                        ).withProperties(
                            iconImage("dash-tesla-supercharger"),
                            iconSize(0.78f),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                        teslaChargingLayer.minZoom = 8f
                        it.addLayer(teslaChargingLayer)

                        map.addOnMapClickListener { latLng ->
                            val screenPoint =
                                map.projection.toScreenLocation(latLng)

                            /*
                             * Nicht nur exakt den berührten Pixel prüfen.
                             * Gerade die kleineren Ladepunkt-Symbole sind
                             * sonst auf dem Display schwer zuverlässig
                             * zu treffen.
                             *
                             * 28 dp Radius = ca. 56 x 56 dp Trefferfläche.
                             */
                            val density =
                                context.resources.displayMetrics.density
                            val radiusPx = 28f * density

                            val touchArea = android.graphics.RectF(
                                screenPoint.x - radiusPx,
                                screenPoint.y - radiusPx,
                                screenPoint.x + radiusPx,
                                screenPoint.y + radiusPx
                            )

                            val features = map.queryRenderedFeatures(
                                touchArea,
                                "charging-tesla-layer",
                                "charging-fast-layer",
                                "charging-normal-layer"
                            )

                            /*
                             * Liegen mehrere Stationen im Trefferbereich,
                             * nehmen wir diejenige, deren Kartenposition
                             * dem tatsächlichen Finger-Tipp am nächsten ist.
                             */
                            val feature = features
                                .mapNotNull { candidate ->
                                    val point =
                                        candidate.geometry() as? Point
                                            ?: return@mapNotNull null

                                    val candidateScreen =
                                        map.projection.toScreenLocation(
                                            LatLng(
                                                point.latitude(),
                                                point.longitude()
                                            )
                                        )

                                    val dx =
                                        candidateScreen.x - screenPoint.x
                                    val dy =
                                        candidateScreen.y - screenPoint.y

                                    candidate to
                                        (dx * dx + dy * dy)
                                }
                                .minByOrNull { it.second }
                                ?.first
                                ?: return@addOnMapClickListener false

                            val geometry = feature.geometry() as? Point
                                ?: return@addOnMapClickListener false

                            fun stringProperty(name: String): String? =
                                if (feature.hasProperty(name)) {
                                    feature.getStringProperty(name)
                                        ?.takeIf { value ->
                                            value.isNotBlank() &&
                                                value != "null"
                                        }
                                } else {
                                    null
                                }

                            fun boolProperty(name: String): Boolean =
                                if (feature.hasProperty(name)) {
                                    runCatching {
                                        feature.getBooleanProperty(name)
                                    }.getOrDefault(false)
                                } else {
                                    false
                                }

                            fun intProperty(name: String): Int? =
                                if (feature.hasProperty(name)) {
                                    runCatching {
                                        feature.getNumberProperty(name)
                                            ?.toInt()
                                    }.getOrNull()
                                } else {
                                    null
                                }

                            fun doubleProperty(name: String): Double? =
                                if (feature.hasProperty(name)) {
                                    runCatching {
                                        feature.getNumberProperty(name)
                                            ?.toDouble()
                                    }.getOrNull()
                                } else {
                                    null
                                }

                            val supercharger =
                                boolProperty("is_supercharger")
                            val fast =
                                boolProperty("is_fast")

                            val stationName =
                                stringProperty("name")
                                    ?: if (supercharger) {
                                        "Tesla Supercharger"
                                    } else if (fast) {
                                        "Schnellladestation"
                                    } else {
                                        "Ladestation"
                                    }

                            currentChargingStationClick(
                                ChargingStationSelection(
                                    lat = geometry.latitude(),
                                    lon = geometry.longitude(),
                                    name = stationName,
                                    operators =
                                        stringProperty("operators"),
                                    chargingPoints =
                                        intProperty("charging_points"),
                                    maxPowerKw =
                                        doubleProperty("max_power_kw"),
                                    connectors =
                                        stringProperty("connectors"),
                                    street =
                                        stringProperty("street"),
                                    houseNumber =
                                        stringProperty("house_number"),
                                    postcode =
                                        stringProperty("postcode"),
                                    city =
                                        stringProperty("city"),
                                    supercharger = supercharger,
                                    fast = fast
                                )
                            )

                            true
                        }
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
                            iconSize(0.72f),
                            iconRotate(Expression.get("bearing")),
                            iconRotationAlignment("map"),
                            iconAllowOverlap(true),
                            iconIgnorePlacement(true)
                        )
                    )
                    mapState.value = map
                    map.cameraPosition = CameraPosition.Builder()
                        .target(
                            if (offlineEnabled)
                                LatLng(52.27, 8.67)
                            else LatLng(51.16, 10.45)
                        )
                        .zoom(if (offlineEnabled) 6.0 else 5.0)
                        .build()
                }
            }
        }
    }


    LaunchedEffect(nightMode, mapState.value) {
        val style = mapState.value?.style
            ?: return@LaunchedEffect

        if (style.getSource("offline") == null) {
            return@LaunchedEffect
        }

        fun line(id: String, day: String, night: String) {
            style.getLayer(id)?.setProperties(
                org.maplibre.android.style.layers.PropertyFactory
                    .lineColor(if (nightMode) night else day)
            )
        }

        fun fill(id: String, day: String, night: String) {
            style.getLayer(id)?.setProperties(
                org.maplibre.android.style.layers.PropertyFactory
                    .fillColor(if (nightMode) night else day)
            )
        }

        style.getLayer("offline-background")?.setProperties(
            org.maplibre.android.style.layers.PropertyFactory
                .backgroundColor(
                    if (nightMode) "#182530" else "#F4F3EF"
                )
        )

        fill("offline-landcover", "#DCE8D7", "#243D35")
        fill("offline-water", "#B5D8E8", "#183F56")
        fill("offline-buildings", "#D8D6D1", "#34404B")

        val roads = listOf(
            Triple("minor", "#FFFFFF", "#344554"),
            Triple("tertiary", "#FFFFFF", "#405263"),
            Triple("secondary", "#F8E1AA", "#85734E"),
            Triple("primary", "#F5D18B", "#94784E"),
            Triple("trunk", "#EBB677", "#A17B50"),
            Triple("motorway", "#E8A66B", "#A77A4F")
        )

        roads.forEach { (name, day, night) ->
            line("dc-road-$name", day, night)
            line("dc-road-$name-outline", "#C0BEBA", "#253442")
        }

        for ((id, day, night) in listOf(
            Triple("offline-street-names", "#344451", "#D7DFE6"),
            Triple("offline-place-names", "#263D50", "#E4EAF0")
        )) {
            style.getLayer(id)?.setProperties(
                org.maplibre.android.style.layers.PropertyFactory
                    .textColor(if (nightMode) night else day),
                org.maplibre.android.style.layers.PropertyFactory
                    .textHaloColor(
                        if (nightMode) "#182530" else "#FFFFFF"
                    )
            )
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

    LaunchedEffect(
        routePoints,
        routeGeneration,
        mapState.value
    ) {
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

        if (map == null) {
            return@LaunchedEffect
        }

        // Gesamtstrecke nur einmal pro bewusst gestarteter
        // Routengeneration anzeigen. Automatisches Rerouting
        // verändert routePoints, aber nicht routeGeneration.
        if (lastOverviewGeneration == routeGeneration) {
            return@LaunchedEffect
        }

        lastOverviewGeneration = routeGeneration
        centered = false

        val bounds = LatLngBounds.Builder().apply {
            routePoints.forEach {
                include(LatLng(it.lat, it.lon))
            }
        }.build()

        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 110)
        )

        delay(4_000L)
        centered = true
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
        var lastSimulationTick = 0L

        while (true) {
            val simulationNow =
                android.os.SystemClock.elapsedRealtime()

            if (NavigationSimulation.active) {
                val simulationDt =
                    if (lastSimulationTick == 0L) {
                        0.05
                    } else {
                        ((simulationNow - lastSimulationTick) / 1000.0)
                            .coerceIn(0.0, 0.25)
                    }

                lastSimulationTick = simulationNow

                if (!NavigationSimulation.paused &&
                    !NavigationSimulation.gpsOutage) {
                    NavigationSimulation.advance(simulationDt)
                }

                // Beim Simulator immer automatisch dem Fahrzeug folgen.
                centered = true
            } else {
                lastSimulationTick = 0L
            }
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

            val liveFix =
                if (NavigationSimulation.active) {
                    NavigationSimulation.fix
                        ?.takeUnless {
                            NavigationSimulation.gpsOutage
                        }
                } else {
                    currentNavigationFix(phoneState.value)
                }

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
                    if (distance <= 500.0) nearTurn = true
                    if (distance >= 550.0) nearTurn = false
                }

                val phone = phoneState.value
                val phoneSpeed = phone
                    ?.takeIf {
                        it.hasSpeed() &&
                        it.speed >= 0f &&
                        navigationGpsIsFresh(
                            it.elapsedRealtimeNanos,
                            android.os.SystemClock.elapsedRealtimeNanos()
                        )
                    }
                    ?.speed
                    ?.toDouble()
                    ?.times(3.6)

                val speedKmh = if (NavigationSimulation.active) {
                    NavigationSimulation.speedKmh.toDouble()
                } else {
                    phoneSpeed ?: 0.0
                }

                if (centered || NavigationSimulation.active) {
                    // Stufenlose Geschwindigkeitskurve.
                    val cruisingZoom = when {
                    speedKmh <= 30.0 -> 16.0

                    speedKmh <= 50.0 ->
                        16.0 -
                            (speedKmh - 30.0) / 20.0

                    speedKmh <= 70.0 ->
                        15.0 -
                            (speedKmh - 50.0) * 1.5 / 20.0

                    speedKmh <= 90.0 ->
                        13.5 -
                            (speedKmh - 70.0) * 1.5 / 20.0

                    else -> 12.0
                }

                val targetZoom = when {
                    currentRoutePoints.size < 2 -> 15.5
                    nearTurn -> 16.0
                    else -> cruisingZoom
                }


                /*
                 * Die Karte läuft optisch unter den rechten KPI-Bereich.
                 * Für die Navigationskamera zählt aber nur der freie
                 * Kartenbereich links davon.
                 *
                 * Dadurch bleibt die Fahrzeugposition unabhängig von
                 * Zoom und Fahrtrichtung stabil im sichtbaren Bereich.
                 */
                if (mapView.width > 0) {
                    map.setPadding(
                        (mapView.width * 0.14f).toInt(),
                        0,
                        (mapView.width * 0.506f).toInt(),
                        0
                    )
                }

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
                // Blickpunkt vor das Fahrzeug verschieben.
                // Bei STOPP (ohne Route) normal zentrieren.
                val previewPixels =
                    if (currentRoutePoints.size >= 2)
                        mapView.height * 0.03
                    else 0.0

                val metersPerPixel =
                    156543.03392 *
                    kotlin.math.cos(
                        Math.toRadians(cameraGps.first)
                    ) / Math.pow(2.0, zoom)

                val ahead = previewPixels * metersPerPixel

                val directionRad =
                    Math.toRadians(camera.bearing)

                val followLat = cameraGps.first +
                    ahead * kotlin.math.cos(directionRad) /
                    111320.0

                val followLon = cameraGps.second +
                    ahead * kotlin.math.sin(directionRad) /
                    (
                        111320.0 * kotlin.math.cos(
                            Math.toRadians(cameraGps.first)
                        ).coerceAtLeast(0.01)
                    )

                val lat = target.latitude +
                    (followLat - target.latitude) *
                    blend(220.0)
                val lon = target.longitude +
                    (followLon - target.longitude) *
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

                // Kleine Richtungsschwankungen ignorieren.
                // Kleine Kurven und GPS-Schwankungen ignorieren.
                if (kotlin.math.abs(delta) < 15.0) {
                    delta = 0.0
                }

                val bearing = camera.bearing +
                    delta * blend(1200.0)

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
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.56f)
                .fillMaxHeight()
        ) {
            /*
             * Verkehrsmeldung erneut anzeigen.
             * Der Button bleibt sichtbar, ist aber ohne Meldungen gedimmt.
             */
            val hasTrafficEvents =
                TrafficEventState.events.isNotEmpty()

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 8.dp,
                        bottom = 12.dp
                    )
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (hasTrafficEvents) {
                            Color(0xFF172B40)
                        } else {
                            Color(0xFF172B40)
                                .copy(alpha = 0.45f)
                        }
                    )
                    .clickable(
                        enabled = hasTrafficEvents
                    ) {
                        TrafficEventState.requestReplay()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠",
                    color =
                        if (hasTrafficEvents) {
                            Color(0xFFFFC66D)
                        } else {
                            Color(0xFF6F7C87)
                        },
                    fontSize = 23.sp
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(
                        end = 60.dp,
                        bottom = 12.dp
                    )
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
                                        bounds, 110
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
}
