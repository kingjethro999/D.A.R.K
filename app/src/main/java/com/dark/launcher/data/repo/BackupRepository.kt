package com.dark.launcher.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

private val Context.settingsDataStore by preferencesDataStore(name = "dark_settings")
private val Context.aliasDataStore by preferencesDataStore(name = "dark_aliases")

@Serializable
data class DarkBackup(
    val version: Int = 2,
    val hiddenApps: Set<String> = emptySet(),
    val vaultApps: Set<String> = emptySet(),
    val vaultLocked: Boolean = false,
    val pinHash: String = "",
    val showTime: Boolean = true,
    val showGit: Boolean = true,
    val showFitness: Boolean = true,
    val showWidgets: Boolean = true,
    val hideRecPostSheet: Boolean = false,
    val recOverlaySize: Int = 56,
    val hiddenGesture: String = "triple_tap",
    val lockGesture: String = "double_tap",
    val profileMode: String = "normal",
    val stepsSource: String = "sensor",
    val githubToken: String = "",
    val groqApiKey: String = "",
    val firecrawlApiKey: String = "",
    val askProvider: String = "groq",
    val askModel: String = "",
    val askApiKey: String = "",
    val webScrapeEnabled: Boolean = true,
    val modes: List<DarkMode> = emptyList(),
    val activeMode: String = "default",
    val nightAllowlist: Set<String> = emptySet(),
    val aliases: Map<String, String> = emptyMap()
)

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: LauncherSettingsRepository,
    private val aliases: AliasRepository
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    suspend fun export(): String {
        val prefs = context.settingsDataStore.data.first()
        val pin = prefs[stringPreferencesKey("pin")] ?: LauncherSettingsRepository.DEFAULT_PIN
        val backup = DarkBackup(
            hiddenApps = prefs[stringSetPreferencesKey("hidden_apps")] ?: emptySet(),
            vaultApps = prefs[stringSetPreferencesKey("vault_apps")] ?: emptySet(),
            vaultLocked = prefs[booleanPreferencesKey("vault_locked")] ?: false,
            pinHash = sha256(pin),
            showTime = prefs[booleanPreferencesKey("show_time")] ?: true,
            showGit = prefs[booleanPreferencesKey("show_git")] ?: true,
            showFitness = prefs[booleanPreferencesKey("show_fitness")] ?: true,
            showWidgets = prefs[booleanPreferencesKey("show_widgets")] ?: true,
            hideRecPostSheet = prefs[booleanPreferencesKey("hide_rec_post_sheet")] ?: false,
            recOverlaySize = prefs[intPreferencesKey("rec_overlay_size")] ?: 56,
            hiddenGesture = prefs[stringPreferencesKey("hidden_apps_gesture")] ?: "triple_tap",
            lockGesture = prefs[stringPreferencesKey("lock_gesture")] ?: "double_tap",
            profileMode = prefs[stringPreferencesKey("profile_mode")] ?: "normal",
            stepsSource = prefs[stringPreferencesKey("steps_source")] ?: "sensor",
            githubToken = prefs[stringPreferencesKey("github_token")] ?: "",
            groqApiKey = prefs[stringPreferencesKey("groq_api_key")] ?: "",
            firecrawlApiKey = prefs[stringPreferencesKey("firecrawl_api_key")] ?: "",
            askProvider = prefs[stringPreferencesKey("ask_provider")] ?: "groq",
            askModel = prefs[stringPreferencesKey("ask_model")] ?: "",
            askApiKey = prefs[stringPreferencesKey("ask_api_key")] ?: "",
            webScrapeEnabled = prefs[booleanPreferencesKey("web_scrape_enabled")] ?: true,
            modes = prefs[stringPreferencesKey("modes")]?.let {
                runCatching { json.decodeFromString<List<DarkMode>>(it) }.getOrNull()
            } ?: emptyList(),
            activeMode = prefs[stringPreferencesKey("active_mode")] ?: "default",
            nightAllowlist = prefs[stringSetPreferencesKey("night_allowlist")] ?: emptySet(),
            aliases = aliases.all()
        )
        return json.encodeToString(backup)
    }

    suspend fun exportToFile(): File {
        val dir = File(context.getExternalFilesDir(null), "backup").apply { mkdirs() }
        val file = File(dir, "dark_backup_${System.currentTimeMillis()}.json")
        file.writeText(export())
        return file
    }

    suspend fun importFromJson(raw: String): Result<Int> = runCatching {
        val backup = json.decodeFromString<DarkBackup>(raw)
        context.settingsDataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("hidden_apps")] = backup.hiddenApps
            prefs[stringSetPreferencesKey("vault_apps")] = backup.vaultApps
            prefs[booleanPreferencesKey("vault_locked")] = backup.vaultLocked
            prefs[booleanPreferencesKey("show_time")] = backup.showTime
            prefs[booleanPreferencesKey("show_git")] = backup.showGit
            prefs[booleanPreferencesKey("show_fitness")] = backup.showFitness
            prefs[booleanPreferencesKey("show_widgets")] = backup.showWidgets
            prefs[booleanPreferencesKey("hide_rec_post_sheet")] = backup.hideRecPostSheet
            prefs[intPreferencesKey("rec_overlay_size")] = backup.recOverlaySize
            prefs[stringPreferencesKey("hidden_apps_gesture")] = backup.hiddenGesture
            prefs[stringPreferencesKey("lock_gesture")] = backup.lockGesture
            prefs[stringPreferencesKey("profile_mode")] = backup.profileMode
            prefs[stringPreferencesKey("steps_source")] = backup.stepsSource
            prefs[stringPreferencesKey("github_token")] = backup.githubToken
            prefs[stringPreferencesKey("groq_api_key")] = backup.groqApiKey
            prefs[stringPreferencesKey("firecrawl_api_key")] = backup.firecrawlApiKey
            prefs[stringPreferencesKey("ask_provider")] = backup.askProvider
            prefs[stringPreferencesKey("ask_model")] = backup.askModel
            prefs[stringPreferencesKey("ask_api_key")] = backup.askApiKey
            prefs[booleanPreferencesKey("web_scrape_enabled")] = backup.webScrapeEnabled
            if (backup.modes.isNotEmpty()) {
                prefs[stringPreferencesKey("modes")] = json.encodeToString(backup.modes)
            }
            prefs[stringPreferencesKey("active_mode")] = backup.activeMode
            if (backup.nightAllowlist.isNotEmpty()) {
                prefs[stringSetPreferencesKey("night_allowlist")] = backup.nightAllowlist
            }
        }
        context.aliasDataStore.edit { prefs ->
            prefs[stringPreferencesKey("aliases_json")] = Json.encodeToString(backup.aliases)
        }
        backup.hiddenApps.size + backup.vaultApps.size + backup.aliases.size + backup.modes.size
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(input.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}
