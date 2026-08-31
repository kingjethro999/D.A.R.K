package com.dark.launcher.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.data.model.GitStats
import com.dark.launcher.data.repo.AppRepository
import com.dark.launcher.data.repo.BackupRepository
import com.dark.launcher.data.repo.GitHubRepository
import com.dark.launcher.data.repo.HealthConnectRepository
import com.dark.launcher.data.repo.LauncherSettingsRepository
import com.dark.launcher.data.repo.MediaRepository
import com.dark.launcher.data.repo.ProfileMode
import com.dark.launcher.data.repo.StepSensorRepository
import com.dark.launcher.data.repo.StepsSource
import com.dark.launcher.data.repo.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
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
    private val media: MediaRepository,
    private val backup: BackupRepository
) : ViewModel() {

    data class SettingsUiState(
        val showTime: Boolean = true,
        val showGit: Boolean = true,
        val showFitness: Boolean = true,
        val showWidgets: Boolean = true,
        val gitToken: String = "",
        val groqApiKey: String = "",
        val firecrawlApiKey: String = "",
        val askProvider: com.dark.launcher.data.repo.AskProvider = com.dark.launcher.data.repo.AskProvider.GROQ,
        val askModel: String = "",
        val askApiKey: String = "",
        val webScrapeEnabled: Boolean = true,
        val modes: List<com.dark.launcher.data.repo.DarkMode> = emptyList(),
        val activeMode: String = "default",
        val gitStats: GitStats = GitStats(),
        val allApps: List<AppInfo> = emptyList(),
        val hiddenApps: Set<String> = emptySet(),
        val vaultApps: Set<String> = emptySet(),
        val vaultLocked: Boolean = false,
        val hiddenGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.TRIPLE_TAP,
        val lockGesture: com.dark.launcher.util.DarkGesture = com.dark.launcher.util.DarkGesture.DOUBLE_TAP,
        val stepsToday: Int = 0,
        val stepsSource: StepsSource = StepsSource.SENSOR,
        val profileMode: ProfileMode = ProfileMode.NORMAL,
        val hideRecPostSheet: Boolean = false,
        val recOverlaySize: Int = com.dark.launcher.data.repo.LauncherSettingsRepository.DEFAULT_OVERLAY_SIZE,
        val healthConnected: Boolean = false,
        val versionName: String = "1.0.0",
        val versionCode: Int = 0,
        val setupDone: Boolean = false,
        val message: String? = null
    )

    private val message = MutableStateFlow<String?>(null)
    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    private val steps = MutableStateFlow(0)
    private val stepsGranted = MutableStateFlow(false)
    private val healthConnected = MutableStateFlow(false)
    private val version = MutableStateFlow(Pair("1.0.0", 0))
    private val setupTick = MutableStateFlow(0)

    private val displayPrefs = combine(
        settings.showTimeFlow,
        settings.showGitFlow,
        settings.showFitnessFlow,
        settings.showWidgetsFlow
    ) { showTime, showGit, showFitness, showWidgets ->
        listOf(showTime, showGit, showFitness, showWidgets)
    }

    private val apiKeys = combine(
        listOf<kotlinx.coroutines.flow.Flow<Any>>(
            settings.gitTokenFlow,
            settings.groqApiKeyFlow,
            settings.firecrawlApiKeyFlow,
            settings.gitStatsFlow,
            settings.askProviderFlow,
            settings.askModelFlow,
            settings.askApiKeyFlow,
            settings.webScrapeEnabledFlow
        )
    ) { args ->
        listOf(
            args[0],
            args[1],
            args[2],
            args[3],
            args[4],
            args[5],
            args[6],
            args[7]
        )
    }

    private val security = combine(
        listOf<kotlinx.coroutines.flow.Flow<Any>>(
            settings.hiddenAppsFlow,
            settings.vaultAppsFlow,
            settings.vaultLockedFlow,
            settings.profileModeFlow,
            settings.hideRecPostSheetFlow,
            settings.stepsSourceFlow,
            settings.recOverlaySizeFlow,
            settings.modesFlow,
            settings.activeModeFlow
        )
    ) { args ->
        listOf(
            args[0],
            args[1],
            args[2],
            args[3],
            args[4],
            args[5],
            args[6],
            args[7],
            args[8]
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

    private val live = combine(steps, healthConnected, _apps, message, version) {
            stepCount, hConnected, apps, msg, ver ->
        listOf(stepCount, hConnected, apps, msg, ver.first, ver.second)
    }

    private val setupDoneFlow = combine(
        settings.pinChangedFlow,
        setupTick
    ) { pinChanged, _ ->
        val overlay = runCatching { android.provider.Settings.canDrawOverlays(context) }.getOrDefault(false)
        val notif = runCatching { media.hasNotificationAccess() }.getOrDefault(false)
        val stepsGranted = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            runCatching {
                context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
            }.getOrDefault(false)
        } else true
        pinChanged && overlay && notif && stepsGranted
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        listOf<kotlinx.coroutines.flow.Flow<Any>>(
            displayPrefs,
            apiKeys,
            security,
            gestures,
            live,
            setupDoneFlow
        )
    ) { args ->
        val display = args[0] as List<Boolean>
        val keys = args[1] as List<Any>
        val sec = args[2] as List<Any>
        val gesturePair = args[3] as Pair<com.dark.launcher.util.DarkGesture, com.dark.launcher.util.DarkGesture>
        val liveData = args[4] as List<Any>
        val setupDone = args[5] as Boolean
        SettingsUiState(
            showTime = display[0],
            showGit = display[1],
            showFitness = display[2],
            showWidgets = display[3],
            gitToken = keys[0] as String,
            groqApiKey = keys[1] as String,
            firecrawlApiKey = keys[2] as String,
            gitStats = keys[3] as GitStats,
            askProvider = keys[4] as com.dark.launcher.data.repo.AskProvider,
            askModel = keys[5] as String,
            askApiKey = keys[6] as String,
            webScrapeEnabled = keys[7] as Boolean,
            hiddenApps = sec[0] as Set<String>,
            vaultApps = sec[1] as Set<String>,
            vaultLocked = sec[2] as Boolean,
            profileMode = sec[3] as ProfileMode,
            hideRecPostSheet = sec[4] as Boolean,
            stepsSource = sec[5] as StepsSource,
            recOverlaySize = sec[6] as Int,
            modes = sec[7] as List<com.dark.launcher.data.repo.DarkMode>,
            activeMode = sec[8] as String,
            hiddenGesture = gesturePair.first,
            lockGesture = gesturePair.second,
            stepsToday = liveData[0] as Int,
            healthConnected = liveData[1] as Boolean,
            allApps = liveData[2] as List<AppInfo>,
            message = liveData[3] as String?,
            versionName = liveData[4] as String,
            versionCode = liveData[5] as Int,
            setupDone = setupDone
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
            _apps.value = appRepository.loadApps().filter { !it.isInternal }
        }
        viewModelScope.launch {
            healthConnected.value = health.hasStepsPermission()
            stepSensor.start()
            while (true) {
                steps.value = stepSensor.todaySteps.value
                stepsGranted.value = health.hasStepsPermission()
                setupTick.value++
                kotlinx.coroutines.delay(30_000)
            }
        }
    }

    fun healthPermissionContract() = health.permissionContract()
    fun healthAvailable(): Boolean = health.available()
    fun stepsPermission(): String = health.stepsPermission()
    fun healthDataAppIntent(): android.content.Intent? = health.healthDataAppIntent()

    fun refreshHealth() {
        viewModelScope.launch {
            healthConnected.value = health.hasStepsPermission()
            steps.value = stepSensor.todaySteps.value
            setupTick.value++
        }
    }

    fun refreshSetup() {
        setupTick.value++
    }

    fun sensorAvailable(): Boolean = stepSensor.sensor != null
    fun readingSteps(): Boolean = sensorAvailable() && stepsGranted.value
    fun mediaAccessGranted(): Boolean = media.hasNotificationAccess()

    fun openNotificationListenerSettings() {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun ensureStepsRunning() = stepSensor.start()

    fun setShowTime(value: Boolean) = viewModelScope.launch { settings.setShowTime(value) }
    fun setShowGit(value: Boolean) = viewModelScope.launch { settings.setShowGit(value) }
    fun setShowFitness(value: Boolean) = viewModelScope.launch { settings.setShowFitness(value) }
    fun setShowWidgets(value: Boolean) = viewModelScope.launch { settings.setShowWidgets(value) }
    fun setHideRecPostSheet(value: Boolean) = viewModelScope.launch { settings.setHideRecPostSheet(value) }
    fun setRecOverlaySize(value: Int) = viewModelScope.launch { settings.setRecOverlaySize(value) }

    fun updateGitToken(token: String) = viewModelScope.launch { settings.setGitToken(token) }
    fun updateGroqKey(key: String) = viewModelScope.launch { settings.setGroqApiKey(key) }
    fun updateFirecrawlKey(key: String) = viewModelScope.launch { settings.setFirecrawlApiKey(key) }

    fun setAskProvider(provider: com.dark.launcher.data.repo.AskProvider) {
        viewModelScope.launch {
            settings.setAskProvider(provider)
            message.value = "${provider.label} selected"
        }
    }

    fun setAskModel(model: String) = viewModelScope.launch { settings.setAskModel(model) }

    fun setAskApiKey(key: String) = viewModelScope.launch {
        settings.setAskApiKey(key)
        if (key.isNotBlank()) message.value = "ask key saved"
    }

    fun clearAskApiKey() = viewModelScope.launch {
        settings.setAskApiKey("")
        message.value = "ask key removed"
    }

    fun setWebScrapeEnabled(value: Boolean) = viewModelScope.launch { settings.setWebScrapeEnabled(value) }

    fun clearGitToken() = viewModelScope.launch {
        settings.setGitToken("")
        message.value = "git token removed"
    }

    fun clearFirecrawlKey() = viewModelScope.launch {
        settings.setFirecrawlApiKey("")
        message.value = "firecrawl key removed"
    }

    fun addMode(name: String) {
        viewModelScope.launch {
            val current = settings.modesFlow.first()
            if (current.size >= com.dark.launcher.data.repo.LauncherSettingsRepository.MAX_MODES) {
                message.value = "max ${com.dark.launcher.data.repo.LauncherSettingsRepository.MAX_MODES} modes"
                return@launch
            }
            val id = "mode_${System.currentTimeMillis()}"
            settings.setModes(current + com.dark.launcher.data.repo.DarkMode(id, name.trim().ifBlank { "Mode ${current.size + 1}" }))
            message.value = "mode created"
        }
    }

    fun renameMode(id: String, name: String) {
        viewModelScope.launch {
            val current = settings.modesFlow.first()
            settings.setModes(current.map { if (it.id == id) it.copy(name = name.trim().ifBlank { it.name }) else it })
        }
    }

    fun removeMode(id: String) {
        viewModelScope.launch {
            val current = settings.modesFlow.first()
            val next = current.filter { it.id != id }
            settings.setModes(next)
            val active = settings.activeModeFlow.first()
            if (active == id) settings.setActiveMode(com.dark.launcher.data.repo.LauncherSettingsRepository.DEFAULT_MODE_ID)
            message.value = "mode removed"
        }
    }

    fun toggleModeApp(modeId: String, packageName: String) {
        viewModelScope.launch {
            val current = settings.modesFlow.first()
            settings.setModes(
                current.map { mode ->
                    if (mode.id == modeId) {
                        val pkgs = if (packageName in mode.packages) mode.packages - packageName else mode.packages + packageName
                        mode.copy(packages = pkgs)
                    } else mode
                }
            )
        }
    }

    fun setActiveMode(id: String) = viewModelScope.launch {
        settings.setActiveMode(id)
        message.value = "mode activated"
    }

    fun importBackup(uri: android.net.Uri) {
        viewModelScope.launch {
            runCatching {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("cannot read file")
                backup.importFromJson(raw)
            }.onSuccess { count ->
                message.value = "backup restored ($count items)"
            }.onFailure {
                message.value = "import failed: ${it.message}"
            }
        }
    }

    fun setStepsSource(source: StepsSource) = viewModelScope.launch { settings.setStepsSource(source) }

    fun setProfileMode(mode: ProfileMode) {
        viewModelScope.launch { settings.applyProfileMode(mode) }
    }

    fun exportBackup() {
        viewModelScope.launch {
            runCatching {
                val file = backup.exportToFile()
                message.value = "backup saved: ${file.name}"
            }.onFailure {
                message.value = "backup failed: ${it.message}"
            }
        }
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
                settings.cacheGitStats(github.fetchStats(token))
            }.onSuccess { message.value = "synced" }
                .onFailure { message.value = "sync failed: ${it.message}" }
        }
    }

    fun changePin(oldPin: String, newPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!settings.verifyPin(oldPin)) {
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
            settings.setVaultApps(if (packageName in current) current - packageName else current + packageName)
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

    suspend fun verifyPin(pin: String): Boolean = settings.verifyPin(pin)

    fun setHiddenGesture(id: String) {
        viewModelScope.launch {
            val lock = settings.lockGestureFlow.first()
            if (id == lock) settings.setLockGesture(settings.hiddenGestureFlow.first())
            settings.setHiddenGesture(id)
        }
    }

    fun setLockGesture(id: String) {
        viewModelScope.launch {
            val hidden = settings.hiddenGestureFlow.first()
            if (id == hidden) settings.setHiddenGesture(settings.lockGestureFlow.first())
            settings.setLockGesture(id)
        }
    }

    fun toast(msg: String) { message.value = msg }
    fun isDeviceAdminActive(): Boolean = com.dark.launcher.util.isDeviceAdminActive(context)
    fun isAccessibilityEnabled(): Boolean = com.dark.launcher.util.isLockAccessibilityEnabled(context)
    fun deviceAdminEnableIntent(): android.content.Intent = com.dark.launcher.util.deviceAdminEnableIntent(context)

    fun openAccessibilitySettings() {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun openAppDetails() {
        runCatching {
            context.startActivity(
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(android.net.Uri.parse("package:${context.packageName}"))
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
