package de.drivechronik.dash

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun OfflineUpdateIndicator(
    modifier: Modifier = Modifier
) {
    val updates = OfflineUpdateState.updateCount
    val attention = OfflineUpdateState.attentionCount

    if (updates == 0 && attention == 0) {
        return
    }

    val text = when {
        updates == 1 ->
            "1 Offline-Update"

        updates > 1 ->
            "$updates Offline-Updates"

        else ->
            "Offline-Daten prüfen"
    }

    Surface(
        modifier = modifier
            .clickable {
                OfflineUpdateState
                    .requestOpenOfflineMaps()
            },
        color = Color(0xEE21364B),
        shape = RoundedCornerShape(10.dp),
        shadowElevation = 5.dp
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = 10.dp,
                vertical = 6.dp
            )
        ) {
            Text(
                text = "●  ",
                color = Color(0xFFFFB74D),
                fontSize = 12.sp
            )

            Text(
                text = text,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
