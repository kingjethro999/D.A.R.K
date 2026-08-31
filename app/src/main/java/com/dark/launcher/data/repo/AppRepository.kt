package com.dark.launcher.data.repo

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Process
import android.os.UserHandle
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dark.launcher.data.model.AppInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

private val Context.appCacheDataStore by preferencesDataStore(name = "dark_app_cache")

private val appsCacheKey = stringPreferencesKey("apps")

@Singleton
class AppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _appsVersion = MutableStateFlow(0L)
    val appsVersion: StateFlow<Long> = _appsVersion.asStateFlow()

    fun notifyAppsChanged() {
        _appsVersion.value = System.currentTimeMillis()
    }

    suspend fun loadApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val ownPackage = context.packageName
        val ownUser = Process.myUserHandle()

        val allActivities = runCatching { pm.queryIntentActivities(intent, 0) }
            .getOrElse { emptyList() }
            .asSequence()
            .mapNotNull { it.activityInfo }

        val apps = allActivities
            .filter { it.packageName != ownPackage }
            .distinctBy { it.packageName }
            .map { info ->
                AppInfo(
                    name = info.loadLabel(pm)?.toString() ?: info.packageName,
                    packageName = info.packageName
                )
            }
            .toMutableList()

        val ownerPkgs = allActivities
            .filter { info ->
                info.packageName != ownPackage &&
                    info.applicationInfo.flags and
                    (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            }
            .map { it.packageName }
            .distinct()
            .toSet()

        apps.addAll(loadDualApps(launcherApps, ownUser, ownerPkgs))

        val sorted = apps.sortedBy { it.name.lowercase() }
        cacheApps(sorted)
        sorted
    }

    suspend fun cachedApps(): List<AppInfo> {
        val raw = context.appCacheDataStore.data.first()[appsCacheKey] ?: return emptyList()
        val cached = runCatching {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                CachedApp(
                    name = o.getString("n"),
                    packageName = o.getString("p"),
                    isInternal = o.optBoolean("i", false),
                    userString = if (o.has("u")) o.getString("u") else null
                )
            }
        }.getOrElse { return emptyList() }
        if (cached.isEmpty()) return emptyList()

        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val userByString = runCatching { launcherApps.getProfiles() }
            .getOrElse { emptyList() }
            .associateBy { it.toString() }

        return cached.map { c ->
            AppInfo(
                name = c.name,
                packageName = c.packageName,
                isInternal = c.isInternal,
                user = c.userString?.let { userByString[it] }
            )
        }
    }

    suspend fun cacheApps(apps: List<AppInfo>) {
        val arr = JSONArray()
        apps.forEach { app ->
            val o = JSONObject()
            o.put("n", app.name)
            o.put("p", app.packageName)
            if (app.isInternal) o.put("i", true)
            app.user?.let { o.put("u", it.toString()) }
            arr.put(o)
        }
        context.appCacheDataStore.edit { it[appsCacheKey] = arr.toString() }
    }

    private data class CachedApp(
        val name: String,
        val packageName: String,
        val isInternal: Boolean,
        val userString: String?
    )

    private fun loadDualApps(
        launcherApps: LauncherApps,
        ownUser: UserHandle,
        ownerPkgs: Set<String>
    ): List<AppInfo> {
        val profiles = runCatching { launcherApps.getProfiles() }.getOrElse { emptyList() }
        val dualUsers = profiles.filter { it != ownUser }
        if (dualUsers.isEmpty()) return emptyList()

        val dualApps = mutableListOf<AppInfo>()
        for (user in dualUsers) {
            val activities = runCatching { launcherApps.getActivityList(null, user) }
                .getOrElse { emptyList() }
            for (activity in activities) {
                val pkg = activity.applicationInfo.packageName
                if (pkg !in ownerPkgs) continue
                val label = activity.label?.toString() ?: pkg
                dualApps += AppInfo(
                    name = "$label (Dual)",
                    packageName = pkg,
                    user = user
                )
            }
        }
        return dualApps
    }
}
