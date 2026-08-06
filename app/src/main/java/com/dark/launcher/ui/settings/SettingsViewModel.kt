package com.dark.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.model.GitStats
import com.dark.launcher.data.repo.AppRepository
import com.dark.launcher.data.repo.GitHubRepository
import com.dark.launcher.data.repo.HealthConnectRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.MediaRepository
import com.dark.launcher.data.repo.StepSensorRepository
import com.dark.launcher.data.repo.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appRepository: AppRepository,
    private val settings: LauncherSettingsRepository,
    private val github: GitHubRepository,
    private val vault: VaultRepository,
    private val health: HealthConnectRepository,
    private val stepSensor: StepSensorRepository,
    private val media: MediaRepository
) : ViewModel() {

    data class SettingsUiState(
        val showTime: Boolean = true,
        val showGit: Boolean = true,
        val showFitness: Boolean = true,
        val gitToken: String = "",
        val gitStats: GitStats = GitStats(),
        val allApps: List<AppInfo> = emptyList(),
        val hiddenApps: Set<String> = emptySet(),
        val vaultApps: Set<String> = emptySet(),
        val vaultLocked: Boolean = false,
        val hiddenGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.TRIPLE_TAP,
        val lockGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.DOUBLE_TAP,
        val stepsToday: Int = 0,
        val healthConnected: Boolean = false,
        val versionName: String = "1.0.0",
        val versionCode: Int = 0,
        val message: String? = null
    )

    private val message = MutableStateFlow<String?>(null)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val steps = MutableStateFlow(0)
    private val healthConnected = MutableStateFlow(false)

    private val displayPrefs = combine(
        settings.showTimeFlow,
        settings.showGitFlow,
        settings.showFitnessFlow
    ) { showTime, showGit, showFitness -> Triple(showTime, showGit, showFitness) }

    private val security = combine(
        settings.hiddenAppsFlow,
        settings.vaultAppsFlow,
        settings.vaultLockedFlow
    ) { hidden, vaultApps, locked -> Triple(hidden, vaultApps, locked) }

    private val gestures = combine(
        settings.hiddenGestureFlow,
        settings.lockGestureFlow
    ) { hidden, lock ->
        Pair(
            com.dark.launcher.util.DarkGesture.fromId(hidden),
            com.dark.launcher.util.DarkGesture.fromId(lock)
        )
    }

    private val git = combine(settings.gitTokenFlow, settings.gitStatsFlow) { token, stats ->
        Pair(token, stats)
    }

    private val version = MutableStateFlow(Pair("1.0.0", 0))

    private val live = combine(steps, healthConnected, _apps, message, version) {
            stepCount, hConnected, apps, msg, ver ->
        Triple(Pair(Triple(stepCount, hConnected, apps), msg), ver.first, ver.second)
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        displayPrefs,
        git,
        security,
        gestures,
        live
    ) { a, b, c, d, e ->
        val (showTime, showGit, showFitness) = a
        val (token, git) = b
        val (hidden, vaultApps, locked) = c
        val (hiddenG, lockG) = d
        val (pair, versionName, versionCode) = e
        val (health, msg) = pair
        SettingsUiState(
            showTime = showTime,
            showGit = showGit,
            showFitness = showFitness,
            gitToken = token,
            gitStats = git,
            allApps = health.third,
            hiddenApps = hidden,
            vaultApps = vaultApps,
            vaultLocked = locked,
            hiddenGesture = hiddenG,
            lockGesture = lockG,
            stepsToday = health.first,
            healthConnected = health.second,
            versionName = versionName,
            versionCode = versionCode,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        viewModelScope.launch {
            version.value = runCatching {
                val info = context.packageManager.getPackageInfo(context.packageName, 0)
                Pair(info.versionName ?: "1.0.0", info.versionCode)
            }.getOrDefault(Pair("1.0.0", 0))
        }
        viewModelScope.launch {
            val apps = appRepository.loadApps().filter { !it.isInternal }
            _apps.value = apps
        }
        viewModelScope.launch {
            healthConnected.value = health.hasStepsPermission()
            while (true) {
                steps.value = stepSensor.todaySteps.value
                kotlinx.coroutines.delay(60_000)
            }
        }
    }

    fun healthPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
        health.permissionContract()

    fun healthAvailable(): Boolean = health.available()

    fun stepsPermission(): String = health.stepsPermission()

    fun healthDataAppIntent(): android.content.Intent? = health.healthDataAppIntent()

    fun refreshHealth() {
        viewModelScope.launch {
            healthConnected.value = health.hasStepsPermission()
            steps.value = stepSensor.todaySteps.value
        }
    }

    fun sensorAvailable(): Boolean = stepSensor.sensor != null

    fun mediaAccessGranted(): Boolean = media.hasNotificationAccess()

    fun openNotificationListenerSettings() {
        viewModelScope.launch {
            runCatching {
                context.startActivity(
                    android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }

    fun ensureStepsRunning() {
        stepSensor.start()
    }

    fun setShowTime(value: Boolean) = viewModelScope.launch { settings.setShowTime(value) }
    fun setShowGit(value: Boolean) = viewModelScope.launch { settings.setShowGit(value) }
    fun setShowFitness(value: Boolean) = viewModelScope.launch { settings.setShowFitness(value) }

    fun updateGitToken(token: String) {
        viewModelScope.launch { settings.setGitToken(token) }
    }

    fun refreshGit() {
        viewModelScope.launch {
            message.value = "syncing..."
            val token = settings.gitTokenFlow.first()
            if (token.isBlank()) {
                message.value = "no token set"
                return@launch
            }
            runCatching {
                val stats = github.fetchStats(token)
                settings.cacheGitStats(stats)
            }.onSuccess {
                message.value = "synced"
            }.onFailure {
                message.value = "sync failed: ${it.message}"
            }
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val ok = settings.verifyPin(oldPin)
            if (!ok) {
                onResult(false, "wrong current pin")
                return@launch
            }
            settings.setPin(newPin)
            onResult(true, "pin updated")
        }
    }

    fun toggleVaultApp(packageName: String) {
        viewModelScope.launch {
            val current = settings.vaultAppsFlow.first()
            settings.setVaultApps(
                if (packageName in current) current - packageName else current + packageName
            )
        }
    }

    fun lockVault() {
        viewModelScope.launch {
            val pin = settings.pinFlow.first()
            vault.lock(pin).onSuccess { message.value = "vault locked ($it files)" }
                .onFailure { message.value = "vault lock failed: ${it.message}" }
        }
    }

    fun unlockVault() {
        viewModelScope.launch {
            val pin = settings.pinFlow.first()
            vault.unlock(pin).onSuccess { message.value = "vault unlocked ($it files)" }
                .onFailure { message.value = "vault unlock failed: ${it.message}" }
        }
    }

    fun unhideApp(packageName: String) {
        viewModelScope.launch { settings.unhideApp(packageName) }
    }

    suspend fun verifyPin(pin: String): Boolean = settings.verifyPin(pin)

    fun setHiddenGesture(id: String) {
        viewModelScope.launch {
            val lock = settings.lockGestureFlow.first()
            if (id == lock) {
                settings.setLockGesture(settings.hiddenGestureFlow.first())
            }
            settings.setHiddenGesture(id)
        }
    }

    fun setLockGesture(id: String) {
        viewModelScope.launch {
            val hidden = settings.hiddenGestureFlow.first()
            if (id == hidden) {
                settings.setHiddenGesture(settings.lockGestureFlow.first())
            }
            settings.setLockGesture(id)
        }
    }

    fun clearMessage() {
        message.value = null
    }

    fun toast(msg: String) {
        message.value = msg
    }
}
