package de.drivechronik.dash

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drivechronik.dash.routing.OfflineRoute

@Composable
internal fun NavigationSimulationControls(route: OfflineRoute) {
    val sim = NavigationSimulation

    Surface(
        color = Color(0xEE142B40),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (!sim.active) {
                TextButton(onClick = { sim.start(route) }) {
                    Text("SIM START", fontSize = 12.sp, color = Color.White)
                }
            } else {
                TextButton(onClick = { sim.togglePause() }) {
                    Text(
                        if (sim.paused) "WEITER" else "PAUSE",
                        fontSize = 12.sp, color = Color.White
                    )
                }
                TextButton(onClick = { sim.changeSpeed() }) {
                    Text("${sim.speedKmh} km/h", fontSize = 12.sp, color = Color.White)
                }
                TextButton(onClick = { sim.stop() }) {
                    Text("STOPP", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}
