package com.dark.launcher.data.repo

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.aliasDataStore by preferencesDataStore(name = "dark_aliases")

@Singleton
class AliasRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val ALIASES = stringPreferencesKey("aliases_json")
    private val json = Json { ignoreUnknownKeys = true }

    val aliasesFlow: Flow<Map<String, String>> = context.aliasDataStore.data.map { prefs ->
        prefs[ALIASES]?.let { raw ->
            runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrElse { emptyMap() }
        } ?: emptyMap()
    }

    suspend fun setAlias(name: String, command: String) {
        val key = name.trim().lowercase()
        if (key.isBlank() || command.isBlank()) return
        context.aliasDataStore.edit { prefs ->
            val current = prefs[ALIASES]?.let { raw ->
                runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrElse { emptyMap() }
            } ?: emptyMap()
            prefs[ALIASES] = json.encodeToString(current + (key to command.trim()))
        }
    }

    suspend fun removeAlias(name: String) {
        val key = name.trim().lowercase()
        context.aliasDataStore.edit { prefs ->
            val current = prefs[ALIASES]?.let { raw ->
                runCatching { json.decodeFromString<Map<String, String>>(raw) }.getOrElse { emptyMap() }
            } ?: emptyMap()
            prefs[ALIASES] = json.encodeToString(current - key)
        }
    }

    suspend fun resolve(name: String): String? = aliasesFlow.first()[name.trim().lowercase()]

    suspend fun all(): Map<String, String> = aliasesFlow.first()
}
