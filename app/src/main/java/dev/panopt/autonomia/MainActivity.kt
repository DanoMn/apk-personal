package dev.panopt.autonomia

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import dev.panopt.autonomia.ui.anchors.AnchorConfigScreen
import dev.panopt.autonomia.ui.dashboard.DashboardScreen
import dev.panopt.autonomia.ui.dashboard.DashboardViewModel
import dev.panopt.autonomia.ui.dashboard.dashboardPalette
import dev.panopt.autonomia.ui.sobriety.SobrietyConfigScreen
import dev.panopt.autonomia.ui.supports.SupportsConfigScreen
import dev.panopt.autonomia.ui.tasks.TasksScreen

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

            var currentScreen by remember { mutableStateOf(AppScreen.Dashboard) }

            SideEffect {
                applySystemBars(isDarkMode = isDarkMode, bgColor = palette.bgBase.toArgb())
            }

            when (currentScreen) {
                AppScreen.Dashboard -> DashboardScreen(
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
                    onCompleteTask = dashboardViewModel::completeTask,
                    onAddAnchor = dashboardViewModel::addActivityAsAnchor,
                    onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor,
                    onNavigateToAnchorConfig = { currentScreen = AppScreen.AnchorConfig },
                    onNavigateToTasks = { currentScreen = AppScreen.Tasks },
                    onNavigateToSobriety = { currentScreen = AppScreen.Sobriety },
                    onToggleSupport = dashboardViewModel::onToggleSupport,
                    onResetSupportOmissions = dashboardViewModel::resetSupportOmissions,
                    onNavigateToSupportsConfig = { currentScreen = AppScreen.Supports },
                    onAddSupport = dashboardViewModel::addToSupports,
                    onRemoveSupport = dashboardViewModel::removeFromSupports,
                    onToggleAllSupports = dashboardViewModel::toggleAllSupports,
                    onSaveSupportChecklist = dashboardViewModel::saveSupportChecklist,
                )
                AppScreen.AnchorConfig -> AnchorConfigScreen(
                    layers = dashboardState.layers,
                    activityOptions = dashboardState.activityOptions,
                    palette = palette,
                    onAddAnchor = dashboardViewModel::addActivityAsAnchor,
                    onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor,
                    onCreateActivity = dashboardViewModel::createActivity,
                    onDeleteActivity = dashboardViewModel::deleteActivity,
                    onBack = { currentScreen = AppScreen.Dashboard },
                )
                AppScreen.Supports -> SupportsConfigScreen(
                    layers = dashboardState.layers,
                    supportItems = dashboardState.supportItems,
                    supportOptions = dashboardState.activityOptions.filter {
                        !it.isConfigured && it.activityType == ActivitySurface.Support.name
                    },
                    palette = palette,
                    onAddSupport = dashboardViewModel::addToSupports,
                    onCreateSupport = { name, layerId ->
                        dashboardViewModel.createActivity(
                            name = name,
                            layerId = layerId,
                            sessionTargetMinutes = 0,
                            isSecondary = true,
                        )
                    },
                    onRemoveSupport = dashboardViewModel::removeFromSupports,
                    onDeleteActivity = dashboardViewModel::deleteActivity,
                    onBack = { currentScreen = AppScreen.Dashboard },
                )
                AppScreen.Tasks -> TasksScreen(
                    pendingTasks = dashboardState.pendingTasks,
                    completedTasks = dashboardState.completedTasks,
                    layers = dashboardState.layers,
                    palette = palette,
                    onCreateTask = dashboardViewModel::createTask,
                    onCompleteTask = dashboardViewModel::completeTask,
                    onReactivateTask = dashboardViewModel::reactivateTask,
                    onBack = { currentScreen = AppScreen.Dashboard },
                )
                AppScreen.Sobriety -> SobrietyConfigScreen(
                    tracks = dashboardState.sobrietyOptions,
                    palette = palette,
                    onToggleClean = dashboardViewModel::toggleAbstinenceClean,
                    onToggleRelapse = dashboardViewModel::toggleAbstinenceRelapse,
                    onSetTrackActive = dashboardViewModel::setAbstinenceTrackActive,
                    onAddTrack = dashboardViewModel::createCustomAbstinenceTrack,
                    onRemoveTrack = dashboardViewModel::deleteCustomAbstinenceTrack,
                    onBack = { currentScreen = AppScreen.Dashboard },
                )
            }
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

private enum class AppScreen {
    Dashboard,
    AnchorConfig,
    Supports,
    Tasks,
    Sobriety,
}
