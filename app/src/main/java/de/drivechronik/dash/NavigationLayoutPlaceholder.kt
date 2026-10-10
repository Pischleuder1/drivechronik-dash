package de.drivechronik.dash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import de.drivechronik.dash.routing.calculatePreferredRoute

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import de.drivechronik.dash.routing.calculateOfflineRoute
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import de.drivechronik.dash.routing.OfflineRoute
import de.drivechronik.dash.routing.RoutePoint
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

private val navigationCurve = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val inset = minOf(w * 0.12f, h * 0.18f)

    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    lineTo(0f, h)
    cubicTo(0f, h * 0.72f, inset, h * 0.75f,
        inset, h * 0.5f)
    cubicTo(inset, h * 0.25f, 0f, h * 0.28f,
        0f, 0f)
    close()
}

@Composable
internal fun NavigationLayoutPlaceholder(
    nightMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    TrafficDisplaySettings.ensureLoaded(context)
    var avoidHighways by remember {
        mutableStateOf(NavigationHighwaySetting.enabled(context))
    }
    var highwayRequest by remember {
        mutableIntStateOf(0)
    }

    var routePoints by remember {
        mutableStateOf<OfflineRoute?>(null)
    }

    var importedPlanRoutePoints by remember {
        mutableStateOf<List<RoutePoint>>(
            emptyList()
        )
    }

    var importedPlanStopPoints by remember {
        mutableStateOf<List<RoutePoint>>(
            emptyList()
        )
    }

    var manualRouteGeneration by remember {
        mutableIntStateOf(0)
    }

    /*
     * Verkehrsdaten gehören zur aktiven Route und nicht
     * in die GPS-Schleife. Sobald eine Route vorhanden ist,
     * sofort prüfen und anschließend alle 60 Sekunden.
     */
    LaunchedEffect(routePoints) {
        val activeRoute = routePoints

        if (activeRoute == null) {
            TrafficEventState.clear()
            return@LaunchedEffect
        }

        android.util.Log.i(
            "DriveChronikTraffic",
            "Traffic-Monitor gestartet: " +
                "points=${activeRoute.points.size} " +
                "maneuvers=${activeRoute.maneuvers.size}"
        )

        while (true) {
            android.util.Log.i(
                "DriveChronikTraffic",
                "Traffic-Abfrage gestartet"
            )

            val result =
                AutobahnTrafficProvider.loadForRoute(
                    activeRoute
                )

            result.onSuccess { events ->
                TrafficEventState.update(events)

                android.util.Log.i(
                    "DriveChronikTraffic",
                    "Treffer auf Route: ${events.size}"
                )

                events.forEach { event ->
                    android.util.Log.i(
                        "DriveChronikTraffic",
                        "${event.type} " +
                            "${event.road} " +
                            "blocked=${event.blocked} " +
                            "title=${event.title}"
                    )
                }
            }.onFailure {
                android.util.Log.w(
                    "DriveChronikTraffic",
                    "Verkehrsdaten fehlgeschlagen: " +
                        it.javaClass.simpleName +
                        ": " +
                        (it.message ?: "")
                )
            }

            delay(60_000L)
        }
    }

    var selectedChargingStation by remember {
        mutableStateOf<ChargingStationSelection?>(null)
    }

    var selectedTrafficEvent by remember {
        mutableStateOf<TrafficEvent?>(null)
    }

    
    /*
     * Automatische Verkehrswarnung:
     * - je Ereignis nur einmal
     * - automatische Popups schließen nach 6 Sekunden
     */
    var autoShownTrafficEventIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    var autoTrafficPopupId by remember {
        mutableStateOf<String?>(null)
    }
var chargingRouteLoading by remember {
        mutableStateOf(false)
    }

    val (phoneState, _, _) =
        rememberPhoneWeatherLocation(navigation = true)


    /*
     * Bei einer bewusst neu gestarteten Route darf jedes
     * Verkehrsereignis wieder einmal automatisch erscheinen.
     */
    LaunchedEffect(manualRouteGeneration) {
        autoShownTrafficEventIds = emptySet()
        autoTrafficPopupId = null
    }

    /*
     * Automatisches Verkehrs-Popup ca. 2 km vor dem Ereignis.
     *
     * TrafficEventState enthält bereits nur die zur Route
     * passenden Autobahn-Ereignisse.
     */
    LaunchedEffect(routePoints, manualRouteGeneration) {
        if (routePoints == null) {
            return@LaunchedEffect
        }

        while (true) {
            if (
                TrafficDisplaySettings.autoPopupsEnabled &&
                selectedTrafficEvent == null &&
                selectedChargingStation == null
            ) {
                val position =
                    if (NavigationSimulation.active) {
                        NavigationSimulation.fix
                            ?.takeUnless {
                                NavigationSimulation.gpsOutage
                            }
                            ?.point
                    } else {
                        currentNavigationFix(
                            phoneState.value
                        )?.point
                    }

                if (position != null) {
                    val nearest =
                        TrafficEventState.events
                            .asSequence()
                            .filterNot { event ->
                                event.id in autoShownTrafficEventIds
                            }
                            .map { event ->
                                val result = FloatArray(1)

                                android.location.Location.distanceBetween(
                                    position.lat,
                                    position.lon,
                                    event.lat,
                                    event.lon,
                                    result
                                )

                                event to result[0].toDouble()
                            }
                            .filter { (_, distanceMeters) ->
                                distanceMeters <= 2_000.0
                            }
                            .minByOrNull { (_, distanceMeters) ->
                                distanceMeters
                            }

                    if (nearest != null) {
                        val (event, distanceMeters) = nearest

                        autoShownTrafficEventIds =
                            autoShownTrafficEventIds + event.id

                        autoTrafficPopupId = event.id
                        selectedTrafficEvent = event

                        android.util.Log.i(
                            "DriveChronikTraffic",
                            "Auto-Popup ${event.id} bei " +
                                "${distanceMeters.toInt()} m"
                        )
                    }
                }
            }

            delay(500L)
        }
    }

    /*
     * Nur automatisch geöffnete Verkehrspopups nach
     * sechs Sekunden wieder schließen.
     *
     * Manuell geöffnete Popups bleiben offen.
     */
    /*
     * Wird die automatische Einblendung abgeschaltet,
     * ein gerade automatisch geöffnetes Popup sofort
     * schließen. Manuell geöffnete Popups bleiben bestehen.
     */
    LaunchedEffect(
        TrafficDisplaySettings.autoPopupsEnabled
    ) {
        if (
            !TrafficDisplaySettings.autoPopupsEnabled &&
            autoTrafficPopupId != null
        ) {
            val eventId = autoTrafficPopupId

            if (selectedTrafficEvent?.id == eventId) {
                selectedTrafficEvent = null
            }

            autoTrafficPopupId = null
        }
    }

    LaunchedEffect(autoTrafficPopupId) {
        val eventId =
            autoTrafficPopupId
                ?: return@LaunchedEffect

        delay(6_000L)

        if (
            autoTrafficPopupId == eventId &&
            selectedTrafficEvent?.id == eventId
        ) {
            selectedTrafficEvent = null
        }

        if (autoTrafficPopupId == eventId) {
            autoTrafficPopupId = null
        }
    }

    LaunchedEffect(NavigationSimulation.active) {
        while (NavigationSimulation.active) {
            val now = android.os.SystemClock.elapsedRealtime()
            delay(if (NavigationSimulation.paused) 1000L else 100L)
            val later = android.os.SystemClock.elapsedRealtime()
            NavigationSimulation.advance(
                ((later - now) / 1000.0).coerceIn(0.0, 0.25)
            )
        }
    }

    DisposableEffect(Unit) {
        onDispose { NavigationSimulation.stop() }
    }

    val stopNavigation = {
        NavigationSimulation.stop()
        NavigationImportState.stopActive()
        manualRouteGeneration++
        NavigationHintState.clear()
        NavigationSummaryState.clearDestination()
        importedPlanRoutePoints = emptyList()
        importedPlanStopPoints = emptyList()
        routePoints = null
    }

    fun navigateToImportedTarget(
        target: ImportedNavigationStop
    ) {
        val origin =
            currentNavigationFix(
                phoneState.value
            )?.point
                ?: weatherCoordinates(
                    phoneState.value
                )?.let {
                    RoutePoint(
                        it.first,
                        it.second
                    )
                }

        if (origin == null) {
            Toast.makeText(
                context,
                "Navigation nicht möglich: GPS fehlt",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        val destination =
            RoutePoint(
                lat = target.lat,
                lon = target.lon
            )

        scope.launch {
            val result =
                calculatePreferredRoute(
                    context,
                    origin,
                    destination
                )

            result.onSuccess { route ->
                NavigationSimulation.stop()

                manualRouteGeneration++

                NavigationHintState.clear()

                NavigationSummaryState
                    .updateDestination(
                        target.name
                    )

                routePoints = route

                Toast.makeText(
                    context,
                    "Navigation: ${target.name}",
                    Toast.LENGTH_SHORT
                ).show()
            }.onFailure {
                Toast.makeText(
                    context,
                    "Route zu ${target.name} " +
                        "konnte nicht berechnet werden",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    NavigationImportState.pendingPlan?.let { plan ->

        androidx.compose.material3.AlertDialog(
            modifier = Modifier
                .fillMaxWidth(0.76f),
            properties =
                androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false
                ),
            onDismissRequest = {
                NavigationImportState.clear()
            },
            title = {
                Text(
                    "Route aus DriveChronik"
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 430.dp)
                        .verticalScroll(
                            rememberScrollState()
                        ),
                    verticalArrangement =
                        Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        plan.name,
                        fontWeight =
                            FontWeight.Bold
                    )

                    if (plan.stops.isNotEmpty()) {
                        Text(
                            "${plan.stops.size} Ladestopp" +
                                if (
                                    plan.stops.size == 1
                                ) {
                                    ""
                                } else {
                                    "s"
                                }
                        )

                        plan.stops.forEachIndexed {
                                index,
                                stop ->

                            Text(
                                "${index + 1}. " +
                                    stop.name +
                                    (
                                        stop.targetSoc
                                            ?.let {
                                                " · Ziel ${it}%"
                                            }
                                            ?: ""
                                    )
                            )
                        }
                    } else {
                        Text(
                            "Keine Ladestopps"
                        )
                    }

                    Text(
                        "Ziel: ${plan.destination.name}"
                    )
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        NavigationImportState
                            .activate(plan)

                        val origin =
                            currentNavigationFix(
                                phoneState.value
                            )?.point
                                ?: weatherCoordinates(
                                    phoneState.value
                                )?.let {
                                    RoutePoint(
                                        it.first,
                                        it.second
                                    )
                                }

                        importedPlanRoutePoints =
                            buildList {
                                origin?.let {
                                    add(it)
                                }

                                plan.stops.forEach {
                                    add(
                                        RoutePoint(
                                            lat = it.lat,
                                            lon = it.lon
                                        )
                                    )
                                }

                                add(
                                    RoutePoint(
                                        lat =
                                            plan.destination.lat,
                                        lon =
                                            plan.destination.lon
                                    )
                                )
                            }

                        importedPlanStopPoints =
                            buildList {
                                plan.stops.forEach {
                                    add(
                                        RoutePoint(
                                            lat = it.lat,
                                            lon = it.lon
                                        )
                                    )
                                }

                                add(
                                    RoutePoint(
                                        lat =
                                            plan.destination.lat,
                                        lon =
                                            plan.destination.lon
                                    )
                                )
                            }

                        val target =
                            NavigationImportState
                                .currentTarget()

                        if (target == null) {
                            Toast.makeText(
                                context,
                                "DriveChronik-Plan enthält kein Ziel",
                                Toast.LENGTH_LONG
                            ).show()

                            NavigationImportState
                                .stopActive()
                        } else {
                            navigateToImportedTarget(
                                target
                            )
                        }
                    }
                ) {
                    Text("Übernehmen")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        NavigationImportState.clear()
                    }
                ) {
                    Text("Abbrechen")
                }
            }
        )
    }

    Box(
        modifier = modifier
            .padding(vertical = 7.dp)
            .clip(navigationCurve)
            .background(Color(0xFF192B3C))
    ) {
        if (OfflineStartupGate.mapReady) {
        NavigationMap(
            nightMode = nightMode,
            modifier = Modifier.fillMaxSize(),
            routePoints = routePoints?.points.orEmpty(),
            planRoutePoints = importedPlanRoutePoints,
            planStopPoints = importedPlanStopPoints,
            routeGeneration = manualRouteGeneration,
            phoneState = phoneState,
            onChargingStationClick = { station ->
                selectedChargingStation = station
            },
            onTrafficEventClick = { event ->
                autoTrafficPopupId = null
                selectedTrafficEvent = event
            }
        )
        } else {
            androidx.compose.material3.Text(
                text = OfflineStartupGate.mapError
                    ?: "Offlinekarten werden geprüft …",
                modifier = Modifier.align(
                    androidx.compose.ui.Alignment.Center
                )
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    top = 10.dp,
                    start = 8.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavigationSimulationControls(
                routePoints,
                onStop = stopNavigation
            )

            if (routePoints != null) {
                IconButton(
                    onClick = stopNavigation,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            Color(0xFFC0393E),
                            RoundedCornerShape(10.dp)
                        )
                ) {
                    Text(
                        "×",
                        color = Color.White,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            AutobahnAvoidButton(
                active = avoidHighways,
                onClick = {
                    val requested = !avoidHighways
                    avoidHighways = requested
                    NavigationHighwaySetting.set(context, requested)
                    highwayRequest++
                    val request = highwayRequest
                    val previous = routePoints

                    if (previous != null) {
                        val origin =
                            currentNavigationFix(phoneState.value)?.point
                                ?: weatherCoordinates(phoneState.value)?.let {
                                    RoutePoint(it.first, it.second)
                                }
                        val target = previous.destination
                            ?: previous.points.lastOrNull()

                        if (origin == null || target == null) {
                            Toast.makeText(
                                context,
                                "Neuberechnung: GPS fehlt",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            scope.launch {
                                val result = calculatePreferredRoute(
                                    context, origin, target
                                )
                                if (request != highwayRequest ||
                                    routePoints !== previous
                                ) return@launch

                                result.onSuccess { updated ->
                                    NavigationSimulation.followReroute(updated)
                                    manualRouteGeneration++
                                    routePoints = updated
                                }.onFailure {
                                    Toast.makeText(
                                        context,
                                        "Route konnte nicht neu berechnet werden",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    }
                }
            )
        }

        /*
         * Während einer aktiven Fahrsimulation:
         *
         * - oben links klarer Simulation-Hinweis
         * - unten links direkter Geschwindigkeitsumschalter
         */
        if (NavigationSimulation.active) {

            Button(
                onClick = {
                    NavigationSimulation.changeSpeed()
                },
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = 8.dp,
                        bottom = 12.dp
                    )
                    .height(40.dp),
                contentPadding =
                    PaddingValues(
                        horizontal = 14.dp,
                        vertical = 0.dp
                    )
            ) {
                Text(
                    text =
                        "${NavigationSimulation.speedKmh} km/h",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        NavigationGuidance(
            routePoints,
            manualRouteGeneration = manualRouteGeneration,
            phoneState = phoneState,
            onRecalculated = { original, updated ->
                if (routePoints === original) {
                    NavigationSimulation.followReroute(updated)
                    routePoints = updated
                }
            },
            onArrived = { arrived ->
                if (routePoints === arrived) {

                    val activePlan =
                        NavigationImportState
                            .activePlan

                    if (activePlan == null) {
                        stopNavigation()
                    } else {
                        val nextTarget =
                            NavigationImportState
                                .advanceTarget()

                        if (nextTarget == null) {
                            Toast.makeText(
                                context,
                                "DriveChronik-Ziel erreicht",
                                Toast.LENGTH_LONG
                            ).show()

                            stopNavigation()
                        } else {
                            /*
                             * Alte Route sofort entfernen,
                             * damit onArrived während der
                             * Neuberechnung nicht mehrfach
                             * ausgelöst wird.
                             */
                            routePoints = null

                            NavigationHintState.clear()

                            navigateToImportedTarget(
                                nextTarget
                            )
                        }
                    }
                }
            }
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.56f)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 8.dp)
            ) {
                NavigationSearch(
                onRoute = { route ->
                    NavigationSimulation.stop()
                    manualRouteGeneration++
                    NavigationHintState.clear()
                    routePoints = route
                },
                onStop = stopNavigation,
                navigationActive = routePoints != null,
                    phoneState = phoneState
                )
            }
        }

        selectedTrafficEvent?.let { event ->
            TrafficEventPopup(
                event = event,
                onClose = {
                    autoTrafficPopupId = null
                    selectedTrafficEvent = null
                }
            )
        }

        selectedChargingStation?.let { station ->
            ChargingStationMenu(
                station = station,
                loading = chargingRouteLoading,
                onClose = {
                    if (!chargingRouteLoading) {
                        selectedChargingStation = null
                    }
                },
                onNavigate = {
                    if (chargingRouteLoading) {
                        return@ChargingStationMenu
                    }

                    val origin =
                        currentNavigationFix(phoneState.value)?.point

                    if (origin == null) {
                        Toast.makeText(
                            context,
                            "Navigation nicht möglich: GPS fehlt",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@ChargingStationMenu
                    }

                    chargingRouteLoading = true

                    scope.launch {
                        val destination = RoutePoint(
                            lat = station.lat,
                            lon = station.lon
                        )

                        val result = calculatePreferredRoute(
                            context,
                            origin,
                            destination
                        )

                        result.onSuccess { route ->
                            NavigationSimulation.stop()
                            manualRouteGeneration++
                            NavigationHintState.clear()
                            NavigationSummaryState.updateDestination(
                                station.name
                            )
                            routePoints = route
                            selectedChargingStation = null
                        }.onFailure {
                            Toast.makeText(
                                context,
                                "Route zur Ladestation konnte " +
                                    "nicht berechnet werden",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                        chargingRouteLoading = false
                    }
                }
            )
        }

    }
}


@Composable
private fun TrafficEventPopup(
    event: TrafficEvent,
    onClose: () -> Unit
) {
    val accent =
        when {
            event.blocked ||
                event.type == TrafficEventType.CLOSURE ->
                Color(0xFFE84C4C)

            event.type == TrafficEventType.ROADWORK ->
                Color(0xFFFF9800)

            else ->
                Color(0xFFFFC107)
        }

    val symbol =
        when {
            event.blocked ||
                event.type == TrafficEventType.CLOSURE ->
                "⛔"

            event.type == TrafficEventType.ROADWORK ->
                "🚧"

            else ->
                "⚠"
        }

    val typeLabel =
        when {
            event.blocked ||
                event.type == TrafficEventType.CLOSURE ->
                "SPERRUNG"

            event.type == TrafficEventType.ROADWORK ->
                "BAUSTELLE"

            else ->
                "VERKEHRSHINWEIS"
        }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onClose,
        properties =
            androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false
            )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Surface(
                modifier =
                    Modifier
                        .fillMaxWidth(0.58f),
                shape =
                    androidx.compose.foundation.shape.RoundedCornerShape(
                        16.dp
                    ),
                color = Color(0xF21A2B3D),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                androidx.compose.foundation.layout.Column(
                    modifier =
                        Modifier.padding(
                            horizontal = 18.dp,
                            vertical = 14.dp
                        )
                ) {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {
                        Text(
                            text = symbol,
                            fontSize = 28.sp
                        )

                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.size(10.dp)
                        )

                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = typeLabel,
                                color = accent,
                                fontSize = 13.sp,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold
                            )

                            if (event.road.isNotBlank()) {
                                Text(
                                    text = event.road,
                                    color = Color(0xFFB8D9F0),
                                    fontSize = 14.sp
                                )
                            }
                        }

                        Text(
                            text = "×",
                            color = Color.White,
                            fontSize = 30.sp,
                            modifier =
                                Modifier
                                    .clip(CircleShape)
                                    .clickable {
                                        onClose()
                                    }
                                    .padding(
                                        horizontal = 10.dp,
                                        vertical = 2.dp
                                    )
                        )
                    }

                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.size(10.dp)
                    )

                    Text(
                        text = event.title,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight =
                            androidx.compose.ui.text.font.FontWeight.SemiBold
                    )

                    if (event.subtitle.isNotBlank()) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.size(6.dp)
                        )

                        Text(
                            text = event.subtitle,
                            color = Color(0xFFD7E3EC),
                            fontSize = 14.sp,
                            lineHeight = 19.sp
                        )
                    }

                    if (event.blocked) {
                        androidx.compose.foundation.layout.Spacer(
                            modifier = Modifier.size(10.dp)
                        )

                        androidx.compose.material3.Surface(
                            shape =
                                androidx.compose.foundation.shape.RoundedCornerShape(
                                    8.dp
                                ),
                            color = Color(0x33E84C4C)
                        ) {
                            Text(
                                text = "GESPERRT",
                                color = Color(0xFFFF7070),
                                fontSize = 13.sp,
                                fontWeight =
                                    androidx.compose.ui.text.font.FontWeight.Bold,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun ChargingStationMenu(
    station: ChargingStationSelection,
    loading: Boolean,
    onNavigate: () -> Unit,
    onClose: () -> Unit
) {
    val cardColor = Color(0xFF21364B)
    val scrollState = rememberScrollState()

    val streetText = listOfNotNull(
        station.street?.takeIf { it.isNotBlank() },
        station.houseNumber?.takeIf { it.isNotBlank() }
    ).joinToString(" ")

    val cityText = listOfNotNull(
        station.postcode?.takeIf { it.isNotBlank() },
        station.city?.takeIf { it.isNotBlank() }
    ).joinToString(" ")

    val powerText = station.maxPowerKw?.let { power ->
        if (power % 1.0 == 0.0) {
            "${power.toInt()} kW"
        } else {
            String.format(
                java.util.Locale.GERMANY,
                "%.1f kW",
                power
            )
        }
    }

    val typeText = when {
        station.supercharger -> "Supercharger"
        station.fast -> "Schnelllader"
        else -> "Ladestation"
    }

    val infoItems = buildList {
        station.operators
            ?.takeIf { it.isNotBlank() }
            ?.let {
                add("Betreiber" to it)
            }

        station.chargingPoints?.let {
            add("Ladepunkte" to it.toString())
        }

        streetText
            .takeIf { it.isNotBlank() }
            ?.let {
                add("Adresse" to it)
            }

        cityText
            .takeIf { it.isNotBlank() }
            ?.let {
                add("Ort" to it)
            }

        powerText?.let {
            add("Leistung" to it)
        }

        station.connectors
            ?.takeIf { it.isNotBlank() }
            ?.let {
                add("Anschlüsse" to it)
            }

        add("Typ" to typeText)
    }

    androidx.compose.material3.AlertDialog(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .widthIn(max = 760.dp),

        properties = DialogProperties(
            usePlatformDefaultWidth = false
        ),

        onDismissRequest = {
            if (!loading) {
                onClose()
            }
        },

        /*
         * Kopfbereich:
         * oben nur der Typ, darunter der konkrete Standort.
         */
        title = {
            Column(
                verticalArrangement =
                    Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text =
                        if (station.supercharger)
                            "Tesla Supercharger"
                        else if (station.fast)
                            "Schnellladestation"
                        else
                            "Ladestation",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )

                if (
                    station.name.isNotBlank() &&
                    station.name != "Tesla Supercharger"
                ) {
                    Text(
                        text = station.name,
                        fontSize = 15.sp,
                        color = Color(0xFFB7C3CF)
                    )
                }
            }
        },

        /*
         * Nur dieser Mittelteil scrollt.
         * Die beiden Buttons bleiben fest unten.
         */
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(scrollState),
                verticalArrangement =
                    Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "INFORMATIONEN",
                    color =
                        androidx.compose.material3.MaterialTheme
                            .colorScheme.primary,
                    style =
                        androidx.compose.material3.MaterialTheme
                            .typography.labelMedium
                )

                infoItems.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { (label, value) ->
                            ChargingStationInfoCard(
                                label = label,
                                value = value,
                                color = cardColor,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        /*
                         * Bei ungerader Anzahl bleibt die
                         * letzte Karte genau eine halbe
                         * Dialogbreite breit.
                         */
                        if (rowItems.size == 1) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (loading) {
                    Spacer(Modifier.height(2.dp))

                    Text(
                        text = "Route wird berechnet …",
                        color =
                            androidx.compose.material3.MaterialTheme
                                .colorScheme.primary,
                        fontSize = 14.sp
                    )
                }
            }
        },

        confirmButton = {
            androidx.compose.material3.TextButton(
                enabled = !loading,
                onClick = onNavigate
            ) {
                Text(
                    if (loading)
                        "Route wird berechnet …"
                    else
                        "Dorthin navigieren"
                )
            }
        },

        dismissButton = {
            androidx.compose.material3.TextButton(
                enabled = !loading,
                onClick = onClose
            ) {
                Text("Schließen")
            }
        }
    )
}


@Composable
private fun ChargingStationInfoCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier,
        colors = CardDefaults.outlinedCardColors(
            containerColor = color
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 10.dp
                ),
            verticalArrangement =
                Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label.uppercase(
                    java.util.Locale.GERMANY
                ),
                color =
                    androidx.compose.material3.MaterialTheme
                        .colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}


@Composable
private fun AutobahnAvoidButton(
    active: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .background(
                if (active) Color(0xFF276A98)
                else Color(0xEE142B40),
                RoundedCornerShape(10.dp)
            )
    ) {
        Canvas(modifier = Modifier.size(32.dp)) {
            val k = size.width / 72f
            drawRoundRect(
                color = Color(0xFF0755A5),
                cornerRadius = CornerRadius(6 * k)
            )
            val road = Path().apply {
                moveTo(22*k, 60*k)
                lineTo(29*k, 12*k)
                lineTo(34*k, 12*k)
                lineTo(31*k, 60*k)
                close()
                moveTo(50*k, 60*k)
                lineTo(43*k, 12*k)
                lineTo(38*k, 12*k)
                lineTo(41*k, 60*k)
                close()
            }
            drawPath(road, Color.White)
            drawLine(
                Color.White, Offset(11*k, 35*k),
                Offset(27*k, 35*k), 4*k
            )
            drawLine(
                Color.White, Offset(45*k, 35*k),
                Offset(61*k, 35*k), 4*k
            )
            if (active) {
                drawLine(
                    Color(0xFFFF4444),
                    Offset(12*k, 62*k),
                    Offset(60*k, 10*k),
                    strokeWidth = 8*k,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
