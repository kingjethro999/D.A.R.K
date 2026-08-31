package com.dark.launcher.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.model.GitStats
import com.dark.launcher.data.repo.AppRepository
import com.dark.launcher.data.repo.FitnessRepository
import com.dark.launcher.data.repo.FitnessSummary
import com.dark.launcher.data.repo.GitHubRepository
import com.dark.launcher.data.repo.HealthConnectRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.NotificationRepository
import com.dark.launcher.data.repo.ProfileMode
import com.dark.launcher.data.repo.RecentAppsRepository
import com.dark.launcher.data.repo.StepSensorRepository
import com.dark.launcher.data.repo.SystemStateRepository
import com.dark.launcher.data.repo.WidgetRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appRepository: AppRepository,
    private val settings: LauncherSettingsRepository,
    private val fitnessRepository: FitnessRepository,
    private val githubRepository: GitHubRepository,
    private val healthRepository: HealthConnectRepository,
    private val stepSensor: StepSensorRepository,
    private val recentApps: RecentAppsRepository,
    private val widgets: WidgetRepository,
    private val notifications: NotificationRepository,
    val systemState: SystemStateRepository
) : ViewModel() {

    data class HomeUiState(
        val apps: List<AppInfo> = emptyList(),
        val showTime: Boolean = true,
        val showGit: Boolean = true,
        val showFitness: Boolean = true,
        val showWidgets: Boolean = true,
        val gitStats: GitStats = GitStats(),
        val gitOffline: Boolean = false,
        val fitness: FitnessSummary = FitnessSummary(),
        val hiddenGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.TRIPLE_TAP,
        val lockGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.DOUBLE_TAP,
        val healthAvailable: Boolean = false,
        val healthLinked: Boolean = false,
        val showHealthPrompt: Boolean = false,
        val isDefaultLauncher: Boolean = true,
        val modeName: String = "",
        val modeFilters: Boolean = false,
        val pinChanged: Boolean = false
    )

    data class HomeExtras(
        val git: GitStats = GitStats(),
        val fit: FitnessSummary = FitnessSummary(),
        val def: Boolean = true,
        val offline: Boolean = false
    )

    private val allApps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val timeText = MutableStateFlow("--:--")
    private val dateText = MutableStateFlow("")
    private val isDefault = MutableStateFlow(true)
    private val healthAvail = MutableStateFlow(false)
    private val healthLinked = MutableStateFlow(false)
    private val healthPromptShown = MutableStateFlow(false)
    private val gitOffline = MutableStateFlow(false)
    private val searchQuery = MutableStateFlow("")
    private val sessionRecent = MutableStateFlow<List<String>>(emptyList())

    val widgetInfo = widgets.widgets
    val shadeNotifications = notifications.recent
    private val _showShade = MutableStateFlow(false)
    val showShade: StateFlow<Boolean> = _showShade.asStateFlow()

    private val visibleApps = combine(
        allApps,
        settings.hiddenAppsFlow,
        settings.vaultAppsFlow,
        settings.vaultLockedFlow,
        settings.modesFlow,
        settings.activeModeFlow,
        searchQuery,
        sessionRecent,
        recentApps.recentFlow
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val apps = values[0] as List<AppInfo>
        val hidden = values[1] as Set<String>
        val vaultApps = values[2] as Set<String>
        val vaultLocked = values[3] as Boolean
        val modes = values[4] as List<com.dark.launcher.data.repo.DarkMode>
        val activeId = values[5] as String
        val query = values[6] as String
        val session = values[7] as List<String>
        val persisted = values[8] as List<com.dark.launcher.data.repo.RecentEntry>

        val activeMode = modes.find { it.id == activeId }
        val modePackages = activeMode?.packages ?: emptySet()

        val tokens = query.lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val filtered = apps.filter { app ->
            app.packageName !in hidden &&
                !(vaultLocked && app.packageName in vaultApps) &&
                (modePackages.isEmpty() || app.packageName in modePackages || app.isInternal) &&
                (tokens.isEmpty() || matchesSearch(app, tokens))
        }
        if (query.isNotBlank()) return@combine filtered
        val recentPkgs = (session + persisted.map { it.packageName }).distinct()
        val recentAppsList = recentPkgs.mapNotNull { pkg -> filtered.find { it.packageName == pkg } }
        val rest = filtered.filter { it.packageName !in recentPkgs }
        recentAppsList + rest
    }

    private val displayPrefs = combine(
        listOf<kotlinx.coroutines.flow.Flow<Any>>(
            settings.showTimeFlow,
            settings.showGitFlow,
            settings.showFitnessFlow,
            settings.showWidgetsFlow,
            settings.modesFlow,
            settings.activeModeFlow,
            settings.pinChangedFlow
        )
    ) { args ->
        listOf(
            args[0] as Boolean,
            args[1] as Boolean,
            args[2] as Boolean,
            args[3] as Boolean,
            args[4] as List<com.dark.launcher.data.repo.DarkMode>,
            args[5] as String,
            args[6] as Boolean
        )
    }

    private val gestures = combine(
        settings.hiddenGestureFlow,
        settings.lockGestureFlow
    ) { hidden, lock ->
        Pair(
            com.dark.launcher.util.DarkGesture.fromId(hidden),
            com.dark.launcher.util.DarkGesture.fromId(lock)
        )
    }

    private val extras = combine(
        settings.gitStatsFlow,
        fitnessRepository.weeklySummary(),
        isDefault,
        gitOffline
    ) { git, fit, def, off -> HomeExtras(git, fit, def, off) }

    val clock: StateFlow<Pair<String, String>> = combine(timeText, dateText) { time, date -> time to date }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "--:--" to ""
        )

    private val prompt = combine(
        healthAvail,
        healthLinked,
        settings.healthPromptDismissedFlow,
        flowOf(stepSensor.sensor == null)
    ) { available, linked, dismissed, noSensor ->
        noSensor && available && !linked && !dismissed && !healthPromptShown.value
    }

    private val healthState = combine(healthAvail, healthLinked) { a, l -> a to l }

    private val right = combine(extras, prompt, healthState) { e, p, h -> Triple(e, p, h) }

    val uiState: StateFlow<HomeUiState> = combine(
        visibleApps,
        displayPrefs,
        gestures,
        right
    ) { apps, prefs, (hiddenG, lockG), right ->
        @Suppress("UNCHECKED_CAST")
        val prefList = prefs as List<Any>
        val showTime = prefList[0] as Boolean
        val showGit = prefList[1] as Boolean
        val showFitness = prefList[2] as Boolean
        val showWidgets = prefList[3] as Boolean
        val modes = prefList[4] as List<com.dark.launcher.data.repo.DarkMode>
        val activeId = prefList[5] as String
        val pinChanged = prefList[6] as Boolean
        val activeMode = modes.find { it.id == activeId }
        val extras = right.first
        val showPrompt = right.second
        val avail = right.third.first
        val linked = right.third.second
        HomeUiState(
            apps = apps,
            showTime = showTime,
            showGit = showGit,
            showFitness = showFitness,
            showWidgets = showWidgets,
            gitStats = extras.git,
            gitOffline = extras.offline,
            fitness = extras.fit,
            hiddenGesture = hiddenG,
            lockGesture = lockG,
            healthAvailable = avail,
            healthLinked = linked,
            showHealthPrompt = showPrompt,
            isDefaultLauncher = extras.def,
            modeName = activeMode?.name.orEmpty(),
            modeFilters = activeMode?.packages?.isNotEmpty() == true,
            pinChanged = pinChanged
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    init {
        viewModelScope.launch {
            appRepository.appsVersion.flatMapLatest {
                flow {
                    emit(appRepository.cachedApps())
                    emit(appRepository.loadApps())
                }
            }.collect { apps ->
                if (apps.isNotEmpty()) allApps.value = apps
            }
        }
        viewModelScope.launch {
            val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
            val dateFmt = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())
            while (isActive) {
                val now = Date()
                timeText.value = timeFmt.format(now)
                dateText.value = dateFmt.format(now)
                widgets.refresh()
                delay(1000)
            }
        }
        viewModelScope.launch {
            healthAvail.value = healthRepository.available()
            if (healthAvail.value) refreshHealth()
        }
    }

    fun refreshHealth() {
        viewModelScope.launch {
            healthLinked.value = healthRepository.hasStepsPermission()
            fitnessRepository.refresh()
        }
    }

    fun healthPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
        healthRepository.permissionContract()

    fun stepsPermission(): String = healthRepository.stepsPermission()

    fun dismissHealthPrompt() {
        healthPromptShown.value = true
        viewModelScope.launch { settings.setHealthPromptDismissed(true) }
    }

    fun hideApp(packageName: String) {
        viewModelScope.launch { settings.hideApp(packageName) }
    }

    fun addToDistractList(packageName: String) {
        viewModelScope.launch { settings.addToVaultApps(packageName) }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { settings.unhideApp(packageName) }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun refreshIsDefault() {
        isDefault.value = systemState.isDefaultLauncher()
    }

    fun ensureStepsRunning() {
        stepSensor.start()
    }

    suspend fun verifyPin(pin: String): Boolean = settings.verifyPin(pin)

    fun defaultLauncherIntent() = systemState.setDefaultLauncherIntent()

    fun isLockEnabled(): Boolean = systemState.isLockAccessibilityEnabled()

    fun expandNotifications(): Boolean {
        _showShade.value = true
        return systemState.expandNotificationPanel()
    }

    fun dismissShade() {
        _showShade.value = false
    }

    fun recordLaunch(packageName: String) {
        sessionRecent.value = (listOf(packageName) + sessionRecent.value).distinct().take(5)
        viewModelScope.launch { recentApps.recordLaunch(packageName) }
    }

    fun refreshGit() {
        viewModelScope.launch {
            val token = settings.gitTokenFlow.first()
            if (token.isBlank()) return@launch
            runCatching {
                val stats = githubRepository.fetchStats(token)
                settings.cacheGitStats(stats)
                gitOffline.value = false
            }.onFailure {
                gitOffline.value = true
            }
        }
    }

    fun stepsPermissionRequestedFlow() = settings.stepsPermissionRequestedFlow

    fun markStepsPermissionRequested() {
        viewModelScope.launch { settings.setStepsPermissionRequested(true) }
    }

    private fun matchesSearch(app: AppInfo, tokens: List<String>): Boolean {
        val haystack = (app.name + " " + app.packageName).lowercase()
        return tokens.all { haystack.contains(it) }
    }
}
