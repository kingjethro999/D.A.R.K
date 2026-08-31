package com.dark.launcher.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.recentAppsDataStore by preferencesDataStore(name = "dark_recent_apps")

@Serializable
data class RecentEntry(val packageName: String, val timestamp: Long)

@Singleton
class RecentAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val RECENT = stringPreferencesKey("recent_json")
    private val json = Json { ignoreUnknownKeys = true }

    val recentFlow: Flow<List<RecentEntry>> = context.recentAppsDataStore.data.map { prefs ->
        prefs[RECENT]?.let { raw ->
            runCatching { json.decodeFromString<List<RecentEntry>>(raw) }.getOrElse { emptyList() }
        } ?: emptyList()
    }

    suspend fun recordLaunch(packageName: String) {
        if (packageName.isBlank()) return
        context.recentAppsDataStore.edit { prefs ->
            val current = prefs[RECENT]?.let { raw ->
                runCatching { json.decodeFromString<List<RecentEntry>>(raw) }.getOrElse { emptyList() }
            } ?: emptyList()
            val updated = (listOf(RecentEntry(packageName, System.currentTimeMillis())) +
                current.filter { it.packageName != packageName })
                .take(MAX)
            prefs[RECENT] = json.encodeToString(updated)
        }
    }

    companion object {
        private const val MAX = 12
    }
}
