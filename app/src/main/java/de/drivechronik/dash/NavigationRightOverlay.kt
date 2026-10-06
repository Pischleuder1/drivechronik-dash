package de.drivechronik.dash

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

internal data class NavigationHint(
    val id: Long,
    val instruction: String,
    val type: Int,
    val distance: Double,
    val stale: Boolean = false
)

internal data class RoundaboutGraphic(
    val hintId: Long,
    val match: RoundaboutMatch,
    val traversal: RoundaboutTraversal
)

internal object NavigationHintState {
    var hint by mutableStateOf<NavigationHint?>(null)
        private set
    private var sequence = 0L

    var roundaboutGraphic by mutableStateOf<RoundaboutGraphic?>(null)
        private set

    fun setRoundaboutGraphic(
        hintId: Long,
        match: RoundaboutMatch,
        traversal: RoundaboutTraversal
    ) {
        if (hint?.id == hintId && hint?.stale == false) {
            roundaboutGraphic = RoundaboutGraphic(
                hintId, match, traversal
            )
        }
    }


    fun show(instruction: String, type: Int, distance: Double) {
        roundaboutGraphic = null
        hint = NavigationHint(++sequence, instruction, type, distance)
    }

    fun markStale() {
        hint = hint?.copy(stale = true)
    }

    fun updateDistance(distance: Double) {
        val current = hint ?: return
        if (current.distance != distance || current.stale) {
            hint = current.copy(distance = distance, stale = false)
        }
    }

    fun clear(id: Long? = null) {
        if (id == null || hint?.id == id) {
            hint = null
            roundaboutGraphic = null
        }
    }
}

@Composable
internal fun NavigationRightOverlay(
    modifier: Modifier = Modifier
) {
    val hint: NavigationHint? = null
    val summary = NavigationSummaryState.text



    AnimatedVisibility(
        visible = hint != null || summary != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF142B40)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                if (hint != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            navigationTurnArrow(hint.type),
                            color = Color(0xFF65B9FF),
                            fontSize = 60.sp,
                            lineHeight = 66.sp
                        )
                        Spacer(Modifier.width(18.dp))
                        Text(
                            if (hint.distance < 1000)
                                String.format(
                                    Locale.GERMANY, "%.0f m",
                                    hint.distance
                                )
                            else
                                String.format(
                                    Locale.GERMANY, "%.1f km",
                                    hint.distance / 1000
                                ),
                            color = Color(0xFF65B9FF),
                            fontSize = 34.sp
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        hint.instruction,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 4
                    )
                }

                    if (hint?.stale == true) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "GPS pausiert",
                            color = Color(0xFFE9BE78),
                            fontSize = 12.sp
                        )
                    }

                    if (summary != null) {
                        if (hint != null) {
                            Spacer(Modifier.height(8.dp))
                        }
                        Text(
                            summary,
                            color = Color(0xFFB8D9F0),
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
            }
        }
    }
}

internal fun navigationTurnArrow(type: Int): String =
    when (type) {
        1, 2, 3, 7, 8, 17, 22 -> "↑"
        9, 18, 20, 23 -> "↗"
        10, 11 -> "↱"
        12 -> "↶"
        13 -> "↷"
        14, 15 -> "↰"
        16, 19, 21, 24 -> "↖"
        25 -> "⇢"
        26 -> "↻"
        27 -> "↗"
        4, 5, 6 -> "◎"
        else -> "↑"
    }
