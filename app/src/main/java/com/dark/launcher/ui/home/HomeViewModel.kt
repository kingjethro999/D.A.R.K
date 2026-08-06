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
import com.dark.launcher.data.repo.MediaRepository
import com.dark.launcher.data.repo.NowPlaying
import com.dark.launcher.data.repo.StepSensorRepository
import com.dark.launcher.data.repo.SystemStateRepository
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
    private val mediaRepository: MediaRepository,
    private val stepSensor: StepSensorRepository,
    val systemState: SystemStateRepository
) : ViewModel() {

    data class HomeUiState(
        val apps: List<AppInfo> = emptyList(),
        val showTime: Boolean = true,
        val showGit: Boolean = true,
        val showFitness: Boolean = true,
        val gitStats: GitStats = GitStats(),
        val gitOffline: Boolean = false,
        val fitness: FitnessSummary = FitnessSummary(),
        val nowPlaying: NowPlaying? = null,
        val hiddenGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.TRIPLE_TAP,
        val lockGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.DOUBLE_TAP,
        val healthAvailable: Boolean = false,
        val healthLinked: Boolean = false,
        val showHealthPrompt: Boolean = false,
        val isDefaultLauncher: Boolean = true
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

    private val visibleApps = combine(
        allApps,
        settings.hiddenAppsFlow,
        settings.vaultAppsFlow,
        settings.vaultLockedFlow,
        searchQuery
    ) { apps, hidden, vaultApps, vaultLocked, query ->
        val tokens = query.lowercase()
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
        apps.filter { app ->
            app.packageName !in hidden &&
                !(vaultLocked && app.packageName in vaultApps) &&
                (tokens.isEmpty() || matchesSearch(app, tokens))
        }
    }

    private val displayPrefs = combine(
        settings.showTimeFlow,
        settings.showGitFlow,
        settings.showFitnessFlow
    ) { showTime, showGit, showFitness -> Triple(showTime, showGit, showFitness) }

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

    private val right = combine(extras, prompt, healthState, mediaRepository.nowPlaying) { e, p, h, m ->
        Pair(e, Triple(p, h, m))
    }

    val uiState: StateFlow<HomeUiState> = combine(
        visibleApps,
        displayPrefs,
        gestures,
        right
    ) { apps, (showTime, showGit, showFitness), (hiddenG, lockG), right ->
        val extras = right.first
        val promptTriple = right.second
        val showPrompt = promptTriple.first
        val avail = promptTriple.second.first
        val linked = promptTriple.second.second
        val media = promptTriple.third
        HomeUiState(
            apps = apps,
            showTime = showTime,
            showGit = showGit,
            showFitness = showFitness,
            gitStats = extras.git,
            gitOffline = extras.offline,
            fitness = extras.fit,
            nowPlaying = media,
            hiddenGesture = hiddenG,
            lockGesture = lockG,
            healthAvailable = avail,
            healthLinked = linked,
            showHealthPrompt = showPrompt,
            isDefaultLauncher = extras.def
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
                delay(1000)
            }
        }
        viewModelScope.launch {
            healthAvail.value = healthRepository.available()
            if (healthAvail.value) {
                refreshHealth()
            }
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

    fun expandNotifications(): Boolean = systemState.expandNotificationPanel()

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
