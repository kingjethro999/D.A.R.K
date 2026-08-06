package com.dark.launcher.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dark.launcher.data.model.GitStats
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore by preferencesDataStore(name = "dark_settings")

@Singleton
class LauncherSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
    private val VAULT_APPS = stringSetPreferencesKey("vault_apps")
    private val VAULT_LOCKED = booleanPreferencesKey("vault_locked")
    private val PIN = stringPreferencesKey("pin")
    private val GITHUB_TOKEN = stringPreferencesKey("github_token")
    private val SHOW_TIME = booleanPreferencesKey("show_time")
    private val SHOW_GIT = booleanPreferencesKey("show_git")
    private val SHOW_FITNESS = booleanPreferencesKey("show_fitness")
    private val GIT_STATS_CACHE = stringPreferencesKey("git_stats_cache")
    private val HIDDEN_GESTURE = stringPreferencesKey("hidden_apps_gesture")
    private val LOCK_GESTURE = stringPreferencesKey("lock_gesture")
    private val HEALTH_PROMPT_DISMISSED = booleanPreferencesKey("health_prompt_dismissed")
    private val STEPS_PERMISSION_REQUESTED = booleanPreferencesKey("steps_permission_requested")

    val hiddenAppsFlow: Flow<Set<String>> = context.settingsDataStore.data
        .map { it[HIDDEN_APPS] ?: emptySet() }

    val vaultAppsFlow: Flow<Set<String>> = context.settingsDataStore.data
        .map { it[VAULT_APPS] ?: emptySet() }

    val vaultLockedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[VAULT_LOCKED] ?: false }

    val pinFlow: Flow<String> = context.settingsDataStore.data
        .map { it[PIN] ?: DEFAULT_PIN }

    val gitTokenFlow: Flow<String> = context.settingsDataStore.data
        .map { it[GITHUB_TOKEN] ?: "" }

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

    val hiddenGestureFlow: Flow<String> = context.settingsDataStore.data
        .map { it[HIDDEN_GESTURE] ?: com.dark.launcher.util.DarkGesture.TRIPLE_TAP.id }

    val lockGestureFlow: Flow<String> = context.settingsDataStore.data
        .map { it[LOCK_GESTURE] ?: com.dark.launcher.util.DarkGesture.DOUBLE_TAP.id }

    val healthPromptDismissedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[HEALTH_PROMPT_DISMISSED] ?: false }

    val stepsPermissionRequestedFlow: Flow<Boolean> = context.settingsDataStore.data
        .map { it[STEPS_PERMISSION_REQUESTED] ?: false }

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
        context.settingsDataStore.edit { it[PIN] = newPin }
    }

    suspend fun verifyPin(input: String): Boolean =
        (context.settingsDataStore.data.first()[PIN] ?: DEFAULT_PIN) == input

    suspend fun setGitToken(token: String) {
        context.settingsDataStore.edit { it[GITHUB_TOKEN] = token }
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

    suspend fun setVaultApps(apps: Set<String>) {
        context.settingsDataStore.edit { it[VAULT_APPS] = apps }
    }

    suspend fun setVaultLocked(locked: Boolean) {
        context.settingsDataStore.edit { it[VAULT_LOCKED] = locked }
    }

    companion object {
        const val DEFAULT_PIN = "0000"
    }
}
