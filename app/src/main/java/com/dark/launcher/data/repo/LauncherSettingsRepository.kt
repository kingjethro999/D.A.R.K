package com.dark.launcher.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dark.launcher.data.model.GitStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "dark_settings")

enum class ProfileMode(val id: String, val label: String) {
    NORMAL("normal", "Normal"),
    WORK("work", "Work"),
    NIGHT("night", "Night");

    companion object {
        fun fromId(id: String): ProfileMode = entries.find { it.id == id } ?: NORMAL
    }
}

enum class StepsSource(val id: String) {
    SENSOR("sensor"),
    HEALTH_CONNECT("health_connect");

    companion object {
        fun fromId(id: String): StepsSource = entries.find { it.id == id } ?: SENSOR
    }
}

enum class AskProvider(val id: String, val label: String, val defaultModel: String) {
    GROQ("groq", "Groq", "llama-3.3-70b-versatile"),
    OPENAI("openai", "OpenAI", "gpt-4o-mini"),
    ANTHROPIC("anthropic", "Claude", "claude-3-5-sonnet-latest"),
    GEMINI("gemini", "Gemini", "gemini-1.5-flash");

    companion object {
        fun fromId(id: String): AskProvider = entries.find { it.id == id } ?: GROQ
    }
}

@kotlinx.serialization.Serializable
data class DarkMode(
    val id: String,
    val name: String,
    val packages: Set<String> = emptySet()
)

@Singleton
class LauncherSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
    private val VAULT_APPS = stringSetPreferencesKey("vault_apps")
    private val VAULT_LOCKED = booleanPreferencesKey("vault_locked")
    private val PIN = stringPreferencesKey("pin")
    private val PIN_CHANGED = booleanPreferencesKey("pin_changed")
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val GITHUB_TOKEN = stringPreferencesKey("github_token")
    private val GROQ_API_KEY = stringPreferencesKey("groq_api_key")
    private val FIRECRAWL_API_KEY = stringPreferencesKey("firecrawl_api_key")
    private val SHOW_TIME = booleanPreferencesKey("show_time")
    private val SHOW_GIT = booleanPreferencesKey("show_git")
    private val SHOW_FITNESS = booleanPreferencesKey("show_fitness")
    private val SHOW_WIDGETS = booleanPreferencesKey("show_widgets")
    private val GIT_STATS_CACHE = stringPreferencesKey("git_stats_cache")
    private val HIDDEN_GESTURE = stringPreferencesKey("hidden_apps_gesture")
    private val LOCK_GESTURE = stringPreferencesKey("lock_gesture")
    private val HEALTH_PROMPT_DISMISSED = booleanPreferencesKey("health_prompt_dismissed")
    private val STEPS_PERMISSION_REQUESTED = booleanPreferencesKey("steps_permission_requested")
    private val STEPS_SOURCE = stringPreferencesKey("steps_source")
    private val PROFILE_MODE = stringPreferencesKey("profile_mode")
    private val HIDE_REC_POST_SHEET = booleanPreferencesKey("hide_rec_post_sheet")
    private val REC_OVERLAY_SIZE = intPreferencesKey("rec_overlay_size")
    private val NIGHT_ALLOWLIST = stringSetPreferencesKey("night_allowlist")
    private val ASK_PROVIDER = stringPreferencesKey("ask_provider")
    private val ASK_MODEL = stringPreferencesKey("ask_model")
    private val ASK_API_KEY = stringPreferencesKey("ask_api_key")
    private val WEB_SCRAPE_ENABLED = booleanPreferencesKey("web_scrape_enabled")
    private val MODES = stringPreferencesKey("modes")
    private val ACTIVE_MODE = stringPreferencesKey("active_mode")

    val hiddenAppsFlow: Flow<Set<String>> = context.settingsDataStore.data
        .map { it[HIDDEN_APPS] ?: emptySet() }

    val vaultAppsFlow: Flow<Set<String>> = context.settingsDataStore.data
        .map { it[VAULT_APPS] ?: emptySet() }

    val vaultLockedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[VAULT_LOCKED] ?: false }

    val pinFlow: Flow<String> = context.settingsDataStore.data
        .map { it[PIN] ?: DEFAULT_PIN }

    val pinChangedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[PIN_CHANGED] ?: false }

    val onboardingCompleteFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[ONBOARDING_COMPLETE] ?: false }

    val gitTokenFlow: Flow<String> = context.settingsDataStore.data
        .map { it[GITHUB_TOKEN] ?: "" }

    val groqApiKeyFlow: Flow<String> = context.settingsDataStore.data
        .map { it[GROQ_API_KEY] ?: "" }

    val firecrawlApiKeyFlow: Flow<String> = context.settingsDataStore.data
        .map { it[FIRECRAWL_API_KEY] ?: "" }

    val gitStatsFlow: Flow<GitStats> = context.settingsDataStore.data
        .map { prefs ->
            prefs[GIT_STATS_CACHE]?.let { raw ->
                runCatching { Json.decodeFromString<GitStats>(raw) }.getOrNull()
            } ?: GitStats()
        }

    val showTimeFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SHOW_TIME] ?: true }

    val showGitFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SHOW_GIT] ?: true }

    val showFitnessFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SHOW_FITNESS] ?: true }

    val showWidgetsFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[SHOW_WIDGETS] ?: true }

    val hiddenGestureFlow: Flow<String> = context.settingsDataStore.data
        .map { it[HIDDEN_GESTURE] ?: com.dark.launcher.util.DarkGesture.TRIPLE_TAP.id }

    val lockGestureFlow: Flow<String> = context.settingsDataStore.data
        .map { it[LOCK_GESTURE] ?: com.dark.launcher.util.DarkGesture.DOUBLE_TAP.id }

    val healthPromptDismissedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[HEALTH_PROMPT_DISMISSED] ?: false }

    val stepsPermissionRequestedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[STEPS_PERMISSION_REQUESTED] ?: false }

    val stepsSourceFlow: Flow<StepsSource> = context.settingsDataStore.data
        .map { StepsSource.fromId(it[STEPS_SOURCE] ?: StepsSource.SENSOR.id) }

    val profileModeFlow: Flow<ProfileMode> = context.settingsDataStore.data
        .map { ProfileMode.fromId(it[PROFILE_MODE] ?: ProfileMode.NORMAL.id) }

    val hideRecPostSheetFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[HIDE_REC_POST_SHEET] ?: false }

    val recOverlaySizeFlow: Flow<Int> = context.settingsDataStore.data
        .map { it[REC_OVERLAY_SIZE] ?: DEFAULT_OVERLAY_SIZE }

    val nightAllowlistFlow: Flow<Set<String>> = context.settingsDataStore.data
        .map { it[NIGHT_ALLOWLIST] ?: defaultNightAllowlist() }

    val askProviderFlow: Flow<AskProvider> = context.settingsDataStore.data
        .map { AskProvider.fromId(it[ASK_PROVIDER] ?: AskProvider.GROQ.id) }

    val askModelFlow: Flow<String> = context.settingsDataStore.data
        .map { it[ASK_MODEL] ?: AskProvider.GROQ.defaultModel }

    val askApiKeyFlow: Flow<String> = context.settingsDataStore.data
        .map { it[ASK_API_KEY] ?: "" }

    val webScrapeEnabledFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[WEB_SCRAPE_ENABLED] ?: true }

    val modesFlow: Flow<List<DarkMode>> = context.settingsDataStore.data
        .map { prefs ->
            prefs[MODES]?.let { raw ->
                runCatching { Json.decodeFromString<List<DarkMode>>(raw) }.getOrNull()
            } ?: defaultModes()
        }

    val activeModeFlow: Flow<String> = context.settingsDataStore.data
        .map { it[ACTIVE_MODE] ?: DEFAULT_MODE_ID }

    suspend fun setStepsPermissionRequested(value: Boolean) {
        context.settingsDataStore.edit { it[STEPS_PERMISSION_REQUESTED] = value }
    }

    suspend fun setHealthPromptDismissed(value: Boolean) {
        context.settingsDataStore.edit { it[HEALTH_PROMPT_DISMISSED] = value }
    }

    suspend fun setHiddenGesture(id: String) {
        context.settingsDataStore.edit { it[HIDDEN_GESTURE] = id }
    }

    suspend fun setLockGesture(id: String) {
        context.settingsDataStore.edit { it[LOCK_GESTURE] = id }
    }

    suspend fun hideApp(packageName: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[HIDDEN_APPS] = (prefs[HIDDEN_APPS] ?: emptySet()) + packageName
        }
    }

    suspend fun unhideApp(packageName: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[HIDDEN_APPS] = (prefs[HIDDEN_APPS] ?: emptySet()) - packageName
        }
    }

    suspend fun setPin(newPin: String) {
        context.settingsDataStore.edit {
            it[PIN] = newPin
            it[PIN_CHANGED] = true
        }
    }

    suspend fun verifyPin(input: String): Boolean =
        (context.settingsDataStore.data.first()[PIN] ?: DEFAULT_PIN) == input

    suspend fun setGitToken(token: String) {
        context.settingsDataStore.edit { it[GITHUB_TOKEN] = token }
    }

    suspend fun setGroqApiKey(key: String) {
        context.settingsDataStore.edit { it[GROQ_API_KEY] = key.trim() }
    }

    suspend fun setFirecrawlApiKey(key: String) {
        context.settingsDataStore.edit { it[FIRECRAWL_API_KEY] = key.trim() }
    }

    suspend fun cacheGitStats(stats: GitStats) {
        context.settingsDataStore.edit { prefs ->
            prefs[GIT_STATS_CACHE] = Json.encodeToString(GitStats.serializer(), stats)
        }
    }

    suspend fun setShowTime(value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_TIME] = value }
    }

    suspend fun setShowGit(value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_GIT] = value }
    }

    suspend fun setShowFitness(value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_FITNESS] = value }
    }

    suspend fun setShowWidgets(value: Boolean) {
        context.settingsDataStore.edit { it[SHOW_WIDGETS] = value }
    }

    suspend fun setVaultApps(apps: Set<String>) {
        context.settingsDataStore.edit { it[VAULT_APPS] = apps }
    }

    suspend fun setVaultLocked(locked: Boolean) {
        context.settingsDataStore.edit { it[VAULT_LOCKED] = locked }
    }

    suspend fun setOnboardingComplete(value: Boolean) {
        context.settingsDataStore.edit { it[ONBOARDING_COMPLETE] = value }
    }

    suspend fun setStepsSource(source: StepsSource) {
        context.settingsDataStore.edit { it[STEPS_SOURCE] = source.id }
    }

    suspend fun setProfileMode(mode: ProfileMode) {
        context.settingsDataStore.edit { it[PROFILE_MODE] = mode.id }
    }

    suspend fun setHideRecPostSheet(value: Boolean) {
        context.settingsDataStore.edit { it[HIDE_REC_POST_SHEET] = value }
    }

    suspend fun setRecOverlaySize(sizeDp: Int) {
        context.settingsDataStore.edit { it[REC_OVERLAY_SIZE] = sizeDp.coerceIn(40, 96) }
    }

    suspend fun setNightAllowlist(packages: Set<String>) {
        context.settingsDataStore.edit { it[NIGHT_ALLOWLIST] = packages }
    }

    suspend fun setAskProvider(provider: AskProvider) {
        context.settingsDataStore.edit { prefs ->
            prefs[ASK_PROVIDER] = provider.id
            if (prefs[ASK_MODEL].isNullOrBlank()) prefs[ASK_MODEL] = provider.defaultModel
        }
    }

    suspend fun setAskModel(model: String) {
        context.settingsDataStore.edit { it[ASK_MODEL] = model.trim() }
    }

    suspend fun setAskApiKey(key: String) {
        context.settingsDataStore.edit { it[ASK_API_KEY] = key.trim() }
    }

    suspend fun setWebScrapeEnabled(value: Boolean) {
        context.settingsDataStore.edit { it[WEB_SCRAPE_ENABLED] = value }
    }

    suspend fun setModes(modes: List<DarkMode>) {
        context.settingsDataStore.edit { prefs ->
            prefs[MODES] = Json.encodeToString(ListSerializer(DarkMode.serializer()), modes)
        }
    }

    suspend fun setActiveMode(id: String) {
        context.settingsDataStore.edit { it[ACTIVE_MODE] = id }
    }

    suspend fun addToVaultApps(packageName: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[VAULT_APPS] = (prefs[VAULT_APPS] ?: emptySet()) + packageName
        }
    }

    suspend fun applyProfileMode(mode: ProfileMode) {
        setProfileMode(mode)
        when (mode) {
            ProfileMode.NORMAL -> setVaultLocked(false)
            ProfileMode.WORK -> setVaultLocked(true)
            ProfileMode.NIGHT -> setVaultLocked(true)
        }
    }

    companion object {
        const val DEFAULT_PIN = "0000"
        const val DEFAULT_OVERLAY_SIZE = 56
        const val DEFAULT_MODE_ID = "default"
        const val MAX_MODES = 5

        fun defaultModes(): List<DarkMode> = listOf(DarkMode(DEFAULT_MODE_ID, "Default"))

        fun defaultNightAllowlist(): Set<String> = setOf(
            "com.android.dialer",
            "com.google.android.dialer",
            "com.samsung.android.dialer",
            "com.android.mms",
            "com.google.android.apps.messaging",
            "com.samsung.android.messaging"
        )
    }
}
