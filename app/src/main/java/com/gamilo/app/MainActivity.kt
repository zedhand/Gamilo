package com.gamilo.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gamilo.app.ai.HardwareGate
import com.gamilo.app.ai.WhisperTranscriptionEngine
import com.gamilo.app.core.GlobalFilter
import com.gamilo.app.di.AppContainer
import com.gamilo.app.di.LambdaViewModelFactory
import com.gamilo.app.security.BiometricAvailability
import com.gamilo.app.security.BiometricGate
import com.gamilo.app.security.BiometricOutcome
import com.gamilo.app.security.DatabaseUnlocker
import com.gamilo.app.security.DbKeyManager
import com.gamilo.app.security.PassphraseStore
import com.gamilo.app.security.UnlockResult
import com.gamilo.app.security.securityDataStore
import com.gamilo.app.settings.GamiloSettings
import com.gamilo.app.ui.nav.BottomDestination
import com.gamilo.app.ui.nav.GamiloBottomBar
import com.gamilo.app.ui.screens.appointments.AppointmentsScreen
import com.gamilo.app.ui.screens.appointments.AppointmentsViewModel
import com.gamilo.app.ui.screens.home.HomeScreen
import com.gamilo.app.ui.screens.home.HomeViewModel
import com.gamilo.app.ui.screens.hours.HoursScreen
import com.gamilo.app.ui.screens.hours.HoursViewModel
import com.gamilo.app.ui.screens.expenses.ExpensesScreen
import com.gamilo.app.ui.screens.expenses.ExpensesViewModel
import com.gamilo.app.ui.screens.jobs.JobsScreen
import com.gamilo.app.ui.screens.jobs.JobsViewModel
import com.gamilo.app.ui.screens.mileage.MileageScreen
import com.gamilo.app.ui.screens.mileage.MileageViewModel
import com.gamilo.app.ui.screens.settings.SettingsScreen
import com.gamilo.app.ui.screens.settings.SettingsViewModel
import com.gamilo.app.ui.screens.shipping.ShippingScreen
import com.gamilo.app.ui.screens.shipping.ShippingViewModel
import com.gamilo.app.ui.screens.tasks.TasksScreen
import com.gamilo.app.ui.screens.tasks.TasksViewModel
import com.gamilo.app.ui.screens.voicelog.VoiceLogSheet
import com.gamilo.app.ui.screens.voicelog.VoiceLogViewModel
import com.gamilo.app.ui.theme.GamiloColors
import com.gamilo.app.ui.theme.GamiloDimens
import com.gamilo.app.ui.theme.GamiloTheme
import java.util.concurrent.TimeUnit

/** FragmentActivity (not ComponentActivity) — androidx.biometric.BiometricPrompt requires one. */
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — reads Theme.Gamilo.Starting (see themes.xml)
        // and hands the window back to Theme.Gamilo once this activity draws its first frame.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GamiloTheme {
                GamiloGate(this)
            }
        }
    }
}

/**
 * Cold-start gate: derives the SQLCipher passphrase (biometric-authenticated in a real build,
 * see DatabaseUnlocker) and only then builds [AppContainer]. There is no path to [GamiloRoot]
 * that skips this.
 */
@Composable
private fun GamiloGate(activity: FragmentActivity) {
    var container by remember { mutableStateOf<AppContainer?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(retryTrigger) {
        if (container != null) return@LaunchedEffect
        errorMessage = null

        val biometricGate = BiometricGate(activity)
        val availability = biometricGate.checkAvailability()
        // Debug/instrumented-test builds bypass the prompt entirely (compiled out of release —
        // see the buildConfigField in app/build.gradle.kts); a device with nothing enrolled
        // falls back to an unprompted (but still real) Keystore-encrypted passphrase, since an
        // auth-required key could never be satisfied there.
        val useBiometricPrompt = !BuildConfig.SKIP_BIOMETRIC_FOR_TESTS && availability == BiometricAvailability.AVAILABLE
        val keyManager = DbKeyManager(requireUserAuthentication = useBiometricPrompt)
        val passphraseStore = PassphraseStore(activity.securityDataStore)
        val unlocker = DatabaseUnlocker(keyManager, passphraseStore, if (useBiometricPrompt) biometricGate else null)

        when (val result = unlocker.unlock()) {
            is UnlockResult.Ready -> {
                val newContainer = AppContainer(activity, result.passphrase)
                (activity.application as GamiloApplication).attachContainer(newContainer)
                container = newContainer
            }
            is UnlockResult.Cancelled -> errorMessage = "Authentication cancelled."
            is UnlockResult.Failed -> errorMessage = result.message
        }
    }

    val currentContainer = container
    if (currentContainer == null) {
        LockedScreen(errorMessage = errorMessage, onRetry = { retryTrigger++ })
    } else {
        GamiloRoot(activity, currentContainer)
    }
}

private enum class Overlay { NONE, SETTINGS, JOBS, CALENDAR }

@Composable
private fun GamiloRoot(activity: FragmentActivity, container: AppContainer) {
    var currentTab by rememberSaveable(stateSaver = Saver({ it.ordinal }, { BottomDestination.entries[it] })) {
        mutableStateOf(BottomDestination.HOME)
    }
    var overlay by rememberSaveable(stateSaver = Saver({ it.ordinal }, { Overlay.entries[it] })) {
        mutableStateOf(Overlay.NONE)
    }
    // Sticky across tab switches (in-memory only, matching StudioFlow) — the same GlobalFilter
    // instance is threaded into every data-heavy tab below so switching tabs never resets it.
    var globalFilter by remember { mutableStateOf(GlobalFilter()) }
    // Voice Log (Phase 2): a pure hardware capability check, computed once — never affects
    // whether the manual Phase 1 UI is available, only whether the voice accelerator appears.
    val voiceLogEligible = remember { HardwareGate.evaluate(activity).isEligible }
    var showVoiceLog by remember { mutableStateOf(false) }

    val settings by container.settingsStore.settings.collectAsState(initial = GamiloSettings.DEFAULT)
    LaunchedEffect(settings.themeVariant) { GamiloColors.applyTheme(settings.themeVariant) }

    // Light schemes (Drafting Table, Blueprint Reverse, Safety Light) need dark status/nav
    // bar icons for outdoor-readable contrast — enableEdgeToEdge() alone only sets this
    // once at cold start, so it can't react to a theme switch happening later in Settings.
    val view = LocalView.current
    LaunchedEffect(settings.themeVariant.isDark) {
        val window = (view.context as android.app.Activity).window
        val insetsController = WindowCompat.getInsetsController(window, view)
        insetsController.isAppearanceLightStatusBars = !settings.themeVariant.isDark
        insetsController.isAppearanceLightNavigationBars = !settings.themeVariant.isDark
    }

    // Resume-lock gate: re-authenticates only if the app was backgrounded for more than 2
    // minutes (switching to the system Camera during a Shipping scan and back should not force
    // a re-prompt). Unlike the cold-start gate in GamiloGate, this never touches the database
    // passphrase — the Room/SQLCipher connection stays open in memory the whole time; this is
    // purely a UI overlay blocking the screen content until re-verified.
    var isSessionUnlocked by remember { mutableStateOf(true) }
    var backgroundedAtMillis by remember { mutableStateOf<Long?>(null) }
    var resumeTrigger by remember { mutableIntStateOf(0) }
    var relockRetryTrigger by remember { mutableIntStateOf(0) }
    var relockError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> backgroundedAtMillis = System.currentTimeMillis()
                Lifecycle.Event.ON_START -> resumeTrigger++
                else -> {}
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(observer)
        onDispose { ProcessLifecycleOwner.get().lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(resumeTrigger) {
        val backgroundedAt = backgroundedAtMillis
        backgroundedAtMillis = null
        if (backgroundedAt != null && System.currentTimeMillis() - backgroundedAt > TimeUnit.MINUTES.toMillis(2)) {
            isSessionUnlocked = false
        }
    }

    LaunchedEffect(relockRetryTrigger) {
        if (isSessionUnlocked || relockRetryTrigger == 0) return@LaunchedEffect
        relockError = null
        val biometricGate = BiometricGate(activity)
        if (biometricGate.checkAvailability() != BiometricAvailability.AVAILABLE) {
            isSessionUnlocked = true
            return@LaunchedEffect
        }
        biometricGate.authenticate { outcome ->
            when (outcome) {
                BiometricOutcome.Success -> isSessionUnlocked = true
                is BiometricOutcome.Error -> relockError = outcome.message
                BiometricOutcome.Failed -> relockError = "Verification failed."
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            bottomBar = {
                GamiloBottomBar(
                    current = currentTab,
                    onSelect = {
                        currentTab = it
                        overlay = Overlay.NONE
                    },
                )
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                Column(modifier = Modifier.fillMaxSize()) {
                    GamiloHeader(
                        title = when (overlay) {
                            Overlay.SETTINGS -> "Settings"
                            Overlay.JOBS -> "Jobs"
                            Overlay.CALENDAR -> "Calendar"
                            Overlay.NONE -> currentTab.label
                        },
                        showActionLinks = overlay == Overlay.NONE,
                        showCloseLink = overlay != Overlay.NONE,
                        onOpenJobs = { overlay = Overlay.JOBS },
                        onOpenCalendar = { overlay = Overlay.CALENDAR },
                        onOpenSettings = { overlay = Overlay.SETTINGS },
                        onClose = { overlay = Overlay.NONE },
                    )
                    when (overlay) {
                        Overlay.SETTINGS -> {
                            val settingsViewModel: SettingsViewModel = viewModel(
                                factory = LambdaViewModelFactory {
                                    SettingsViewModel(container.settingsStore, container.backupManager, container.database, container.dataExportService)
                                },
                            )
                            SettingsScreen(
                                settingsViewModel,
                                onRequestFactoryResetAuth = { onAuthenticated ->
                                    val biometricGate = BiometricGate(activity)
                                    val useBiometricPrompt = !BuildConfig.SKIP_BIOMETRIC_FOR_TESTS &&
                                        biometricGate.checkAvailability() == BiometricAvailability.AVAILABLE
                                    if (useBiometricPrompt) {
                                        biometricGate.authenticate { outcome ->
                                            if (outcome is BiometricOutcome.Success) onAuthenticated()
                                        }
                                    } else {
                                        onAuthenticated()
                                    }
                                },
                            )
                        }
                        Overlay.JOBS -> {
                            val jobsViewModel: JobsViewModel = viewModel(
                                factory = LambdaViewModelFactory { JobsViewModel(container.jobRepository) },
                            )
                            JobsScreen(jobsViewModel)
                        }
                        Overlay.CALENDAR -> {
                            val appointmentsViewModel: AppointmentsViewModel = viewModel(
                                factory = LambdaViewModelFactory { AppointmentsViewModel(container.appointmentRepository, container.jobRepository) },
                            )
                            AppointmentsScreen(appointmentsViewModel)
                        }
                        Overlay.NONE -> when (currentTab) {
                            BottomDestination.HOME -> {
                                val vm: HomeViewModel = viewModel(
                                    factory = LambdaViewModelFactory {
                                        HomeViewModel(container.jobRepository, container.taskRepository, container.hourRepository, container.settingsStore)
                                    },
                                )
                                HomeScreen(vm, isVoiceLogEligible = voiceLogEligible, onStartVoiceLog = { showVoiceLog = true })
                            }
                            BottomDestination.TASKS -> {
                                val vm: TasksViewModel = viewModel(
                                    factory = LambdaViewModelFactory { TasksViewModel(container.taskRepository, container.jobRepository) },
                                )
                                TasksScreen(vm, globalFilter, onFilterChange = { globalFilter = it })
                            }
                            BottomDestination.HOURS -> {
                                val vm: HoursViewModel = viewModel(
                                    factory = LambdaViewModelFactory { HoursViewModel(container.hourRepository, container.settingsStore, container.jobRepository) },
                                )
                                HoursScreen(vm, globalFilter, onFilterChange = { globalFilter = it })
                            }
                            BottomDestination.EXPENSES -> {
                                val vm: ExpensesViewModel = viewModel(
                                    factory = LambdaViewModelFactory {
                                        ExpensesViewModel(container.expenseRepository, container.attachmentRepository, container.settingsStore, container.jobRepository)
                                    },
                                )
                                ExpensesScreen(vm, globalFilter, onFilterChange = { globalFilter = it })
                            }
                            BottomDestination.MILEAGE -> {
                                val vm: MileageViewModel = viewModel(
                                    factory = LambdaViewModelFactory { MileageViewModel(container.mileageRepository, container.settingsStore, container.jobRepository) },
                                )
                                MileageScreen(vm, globalFilter, onFilterChange = { globalFilter = it })
                            }
                            BottomDestination.LOGISTICS -> {
                                val vm: ShippingViewModel = viewModel(
                                    factory = LambdaViewModelFactory { ShippingViewModel(container.shippingRepository, container.settingsStore, container.jobRepository) },
                                )
                                ShippingScreen(vm, globalFilter, onFilterChange = { globalFilter = it })
                            }
                        }
                    }
                }
            }
        }

        if (!isSessionUnlocked) {
            LockedScreen(errorMessage = relockError, onRetry = { relockRetryTrigger++ })
        }

        if (showVoiceLog) {
            val voiceLogViewModel: VoiceLogViewModel = viewModel(
                factory = LambdaViewModelFactory {
                    VoiceLogViewModel(WhisperTranscriptionEngine(activity.applicationContext), container.jobRepository)
                },
            )
            VoiceLogSheet(viewModel = voiceLogViewModel, onDismiss = { showVoiceLog = false })
        }
    }
}

@Composable
private fun LockedScreen(errorMessage: String?, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(GamiloColors.Background)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onRetry),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = "VERIFY TO CONTINUE", color = GamiloColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            errorMessage?.let { Text(text = it, color = GamiloColors.Accent, fontSize = 13.sp) }
            Text(text = "Tap to retry", color = GamiloColors.TextSecondary, fontSize = 13.sp)
        }
    }
}

@Composable
private fun GamiloHeader(
    title: String,
    showActionLinks: Boolean,
    showCloseLink: Boolean,
    onOpenJobs: () -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(GamiloDimens.TapTargetHeight)
            .background(GamiloColors.Surface)
            .padding(horizontal = GamiloDimens.ScreenPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = title.uppercase(), color = GamiloColors.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        when {
            showCloseLink -> HeaderLink("Close", onClose)
            showActionLinks -> Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                HeaderLink("Jobs", onOpenJobs)
                HeaderLink("Calendar", onOpenCalendar)
                HeaderLink("Settings", onOpenSettings)
            }
        }
    }
}

@Composable
private fun HeaderLink(label: String, onClick: () -> Unit) {
    Text(
        text = label.uppercase(),
        color = GamiloColors.Accent,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
        ),
    )
}
