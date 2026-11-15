package de.drivechronik.dash

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

internal object DashboardLayoutState {

    var mode by mutableStateOf(DashboardLayoutMode.CLASSIC)
        private set

    fun load(context: Context) {
        mode = DashboardLayoutSettings(context).load()
    }

    fun select(
        context: Context,
        selected: DashboardLayoutMode
    ) {
        DashboardLayoutSettings(context).save(selected)
        mode = selected
    }
}
