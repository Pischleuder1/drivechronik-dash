package de.drivechronik.dash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drivechronik.dash.routing.*
import java.util.Locale
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.roundToInt

@Composable
internal fun NavigationGuidancePanel(
    route: OfflineRoute,
    progress: RouteProgress?
) {
    val maneuver = progress?.nextManeuver
    val remaining = progress?.distanceRemainingMeters
        ?: route.distanceKm * 1000.0
    val total = progress?.let {
        it.distanceAlongRouteMeters + remaining
    } ?: route.distanceKm * 1000.0

    val minutes = if (total > 0.0) {
        route.durationSeconds * remaining / total / 60.0
    } else 0.0

    val eta = SimpleDateFormat("HH:mm", Locale.GERMANY)
        .format(Date(System.currentTimeMillis() +
            (minutes * 60000).toLong()))


    val totalMinutes = minutes
        .roundToInt().coerceAtLeast(0)

    val summary =
        "Entfernung: %.1f km\nZeit: %dh %02d Min\nAnkunft: %s Uhr"
            .format(
                Locale.GERMANY,
                remaining / 1000.0,
                totalMinutes / 60,
                totalMinutes % 60,
                eta
            )

    SideEffect {
        NavigationSummaryState.update(summary)
    }

    DisposableEffect(Unit) {
        onDispose {
            NavigationSummaryState.clear()
        }
    }
}
