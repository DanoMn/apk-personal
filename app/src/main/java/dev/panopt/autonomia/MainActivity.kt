package dev.panopt.autonomia

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import dev.panopt.autonomia.ui.dashboard.DashboardScreen
import dev.panopt.autonomia.ui.dashboard.DashboardViewModel
import dev.panopt.autonomia.ui.dashboard.dashboardPalette

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(applicationContext),
            )
            val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
            val isDarkMode by dashboardViewModel.isDarkMode.collectAsStateWithLifecycle()
            val palette = dashboardPalette(isDarkMode)

            SideEffect {
                applySystemBars(isDarkMode = isDarkMode, bgColor = palette.bgBase.toArgb())
            }

            DashboardScreen(
                state = dashboardState,
                isDarkMode = isDarkMode,
                onThemeChange = dashboardViewModel::setDarkMode,
                onToggleActivity = dashboardViewModel::toggleActivity,
                onToggleAbstinenceClean = dashboardViewModel::toggleAbstinenceClean,
                onSaveActivityValue = dashboardViewModel::saveActivityValue,
                onSaveSleep = dashboardViewModel::saveSleep,
                onToggleAbstinenceRelapse = dashboardViewModel::toggleAbstinenceRelapse,
                onCreateActivity = dashboardViewModel::createActivity,
                onSetFocusSignal = dashboardViewModel::setFocusSignalActivity,
                onCreateTask = dashboardViewModel::createTask,
                onCompleteTask = dashboardViewModel::completeTask,
            )
        }
    }

    @Suppress("DEPRECATION")
    private fun applySystemBars(isDarkMode: Boolean, bgColor: Int) {
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        window.decorView.systemUiVisibility = if (isDarkMode) {
            window.decorView.systemUiVisibility and
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv() and
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
        } else {
            window.decorView.systemUiVisibility or
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or
                View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        }
    }
}
