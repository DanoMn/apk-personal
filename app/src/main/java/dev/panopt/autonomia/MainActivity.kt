package dev.panopt.autonomia

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.panopt.autonomia.data.worker.DailyClosureWorkScheduler
import dev.panopt.autonomia.domain.activity.DEFAULT_ANCHOR_SESSION_MINUTES
import dev.panopt.autonomia.domain.activity.DEFAULT_ANCHOR_WEEKLY_FREQUENCY
import dev.panopt.autonomia.sleep.SleepDeviceAdminReceiver
import dev.panopt.autonomia.ui.anchors.AnchorConfigScreen
import dev.panopt.autonomia.ui.dashboard.DashboardScreen
import dev.panopt.autonomia.ui.dashboard.DashboardViewModel
import dev.panopt.autonomia.ui.dashboard.dashboardPalette
import dev.panopt.autonomia.ui.onboarding.OnboardingScreen
import dev.panopt.autonomia.ui.onboarding.OnboardingViewModel
import dev.panopt.autonomia.ui.scoring.ScoringScreen
import dev.panopt.autonomia.ui.sleep.SleepConfigScreen
import dev.panopt.autonomia.ui.sobriety.SobrietyConfigScreen
import dev.panopt.autonomia.ui.supports.SupportsConfigScreen
import dev.panopt.autonomia.ui.tasks.TasksScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DailyClosureWorkScheduler.schedule(applicationContext)

        setContent {
            val devicePolicyManager = remember {
                getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
            }
            val sleepAdmin = remember {
                ComponentName(this@MainActivity, SleepDeviceAdminReceiver::class.java)
            }
            var isSleepLockActive by remember {
                mutableStateOf(devicePolicyManager.isAdminActive(sleepAdmin))
            }
            val adminLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                ActivityResultContracts.StartActivityForResult(),
            ) {
                isSleepLockActive = devicePolicyManager.isAdminActive(sleepAdmin)
            }
            DisposableEffect(Unit) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        isSleepLockActive = devicePolicyManager.isAdminActive(sleepAdmin)
                    }
                }
                lifecycle.addObserver(observer)
                onDispose { lifecycle.removeObserver(observer) }
            }

            val requestSleepLockPermission = {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
                    .putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, sleepAdmin)
                    .putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        getString(R.string.sleep_device_admin_explanation),
                    )
                adminLauncher.launch(intent)
            }
            val lockPhoneNow: () -> Unit = {
                if (devicePolicyManager.isAdminActive(sleepAdmin)) {
                    isSleepLockActive = true
                    runCatching {
                        devicePolicyManager.lockNow()
                    }.onFailure {
                        isSleepLockActive = devicePolicyManager.isAdminActive(sleepAdmin)
                        requestSleepLockPermission()
                    }
                } else {
                    isSleepLockActive = false
                    requestSleepLockPermission()
                }
            }

            val dashboardViewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModel.Factory(applicationContext),
            )
            val dashboardState by dashboardViewModel.dashboardState.collectAsStateWithLifecycle()
            val isDarkMode by dashboardViewModel.isDarkMode.collectAsStateWithLifecycle()
            val isSleepAutoModeEnabled by dashboardViewModel.isSleepAutoModeEnabled.collectAsStateWithLifecycle()
            val palette = dashboardPalette(isDarkMode)

            val onboardingViewModel: OnboardingViewModel = viewModel(
                factory = OnboardingViewModel.Factory(applicationContext),
            )
            val onboardingState by onboardingViewModel.onboardingState.collectAsStateWithLifecycle()

            // Gate de primer-uso: la pantalla inicial se siembra del valor síncrono del
            // estado de onboarding (StateFlow con valor inicial leído de prefs → sin flicker).
            var currentScreen by remember {
                mutableStateOf(
                    if (onboardingViewModel.onboardingState.value.completed) {
                        AppScreen.Dashboard
                    } else {
                        AppScreen.Onboarding
                    },
                )
            }

            SideEffect {
                applySystemBars(isDarkMode = isDarkMode, bgColor = palette.bgBase.toArgb())
            }

            when (currentScreen) {
                AppScreen.Onboarding -> OnboardingScreen(
                    state = onboardingState,
                    palette = palette,
                    onAdvance = onboardingViewModel::advance,
                    onBack = onboardingViewModel::back,
                    onComplete = {
                        onboardingViewModel.complete()
                        currentScreen = AppScreen.Dashboard
                    },
                    layers = dashboardState.layers,
                    anchorOptions = dashboardState.activityOptions,
                    onAddAnchor = { activityId ->
                        dashboardViewModel.addActivityAsAnchor(
                            activityId,
                            DEFAULT_ANCHOR_SESSION_MINUTES,
                            DEFAULT_ANCHOR_WEEKLY_FREQUENCY,
                            null,
                        )
                    },
                    onCreateAnchor = { name, layerId ->
                        dashboardViewModel.createActivity(
                            name = name,
                            layerId = layerId,
                            sessionTargetMinutes = DEFAULT_ANCHOR_SESSION_MINUTES,
                            isSecondary = false,
                            weeklyFrequencyTarget = DEFAULT_ANCHOR_WEEKLY_FREQUENCY,
                            commitmentDurationMonths = null,
                        )
                    },
                    onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor,
                )
                AppScreen.Dashboard -> DashboardScreen(
                    state = dashboardState,
                    isDarkMode = isDarkMode,
                    isSleepLockActive = isSleepLockActive,
                    onThemeChange = dashboardViewModel::setDarkMode,
                    onRequestSleepLockPermission = requestSleepLockPermission,
                    onToggleActivity = dashboardViewModel::toggleActivity,
                    onToggleAbstinenceClean = dashboardViewModel::toggleAbstinenceClean,
                    onSaveActivityValue = dashboardViewModel::saveActivityValue,
                    onStartSleepSession = { dashboardViewModel.startSleepSession(lockPhoneNow) },
                    onFinishSleepSession = dashboardViewModel::finishSleepSession,
                    onToggleAbstinenceRelapse = dashboardViewModel::toggleAbstinenceRelapse,
                    onCreateActivity = dashboardViewModel::createActivity,
                    onSetFocusSignal = dashboardViewModel::setFocusSignalActivity,
                    onCompleteTask = dashboardViewModel::completeTask,
                    onAddAnchor = dashboardViewModel::addActivityAsAnchor,
                    onRemoveAnchor = dashboardViewModel::removeActivityAsAnchor,
                    onNavigateToScoring = { currentScreen = AppScreen.Scoring },
                    onNavigateToAnchorConfig = { currentScreen = AppScreen.AnchorConfig },
                    onNavigateToTasks = { currentScreen = AppScreen.Tasks },
                    onNavigateToSobriety = { currentScreen = AppScreen.Sobriety },
                    onNavigateToSleepConfig = { currentScreen = AppScreen.SleepConfig },
                    onToggleSupport = dashboardViewModel::onToggleSupport,
                    onResetSupportOmissions = dashboardViewModel::resetSupportOmissions,
                    onNavigateToSupportsConfig = { currentScreen = AppScreen.Supports },
                    onAddSupport = dashboardViewModel::addToSupports,
                    onRemoveSupport = dashboardViewModel::removeFromSupports,
                    onToggleAllSupports = dashboardViewModel::toggleAllSupports,
                    onSaveSupportChecklist = dashboardViewModel::saveSupportChecklist,
                )
                AppScreen.Scoring -> ScoringScreen(
                    state = dashboardState,
                    palette = palette,
                    onBack = { currentScreen = AppScreen.Dashboard },
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
                AppScreen.SleepConfig -> SleepConfigScreen(
                    sleep = dashboardState.sleep,
                    isSleepLockActive = isSleepLockActive,
                    isAutoModeEnabled = isSleepAutoModeEnabled,
                    palette = palette,
                    onRequestSleepLockPermission = requestSleepLockPermission,
                    onToggleAutoMode = { enabled, onPermissionRequired ->
                        dashboardViewModel.toggleSleepAutoMode(enabled, onPermissionRequired)
                    },
                    onOpenTelemetrySettings = {
                        startActivity(
                            dev.panopt.autonomia.platform.telemetry.TelemetryPermission.settingsIntent(),
                        )
                    },
                    onOpenAppDetailsSettings = {
                        startActivity(
                            dev.panopt.autonomia.platform.telemetry.TelemetryPermission
                                .appDetailsSettingsIntent(this@MainActivity),
                        )
                    },
                    onSave = dashboardViewModel::saveSleepConfig,
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
    Onboarding,
    Dashboard,
    Scoring,
    AnchorConfig,
    Supports,
    Tasks,
    Sobriety,
    SleepConfig,
}
