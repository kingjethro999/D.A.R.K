package com.dark.launcher.data.repo

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import com.dark.launcher.service.LauncherAccessibilityService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SystemStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun isDefaultLauncher(): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = context.packageManager.resolveActivity(
            intent,
            PackageManager.MATCH_DEFAULT_ONLY
        )
        return resolveInfo?.activityInfo?.packageName == context.packageName
    }

    fun setDefaultLauncherIntent(): Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(Context.ROLE_SERVICE) as android.app.role.RoleManager
        if (roleManager.isRoleAvailable(android.app.role.RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(android.app.role.RoleManager.ROLE_HOME)
        ) {
            roleManager.createRequestRoleIntent(android.app.role.RoleManager.ROLE_HOME)
        } else {
            null
        }
    } else {
        Intent(Settings.ACTION_HOME_SETTINGS)
    }

    fun isLockAccessibilityEnabled(): Boolean =
        isAccessibilityServiceEnabled(LauncherAccessibilityService::class.java)

    fun isAccessibilityServiceEnabled(service: Class<*>): Boolean {
        val expected = ComponentName(context, service)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val component = ComponentName.unflattenFromString(splitter.next())
            if (component != null && component == expected) return true
        }
        return false
    }

    fun accessibilitySettingsIntent(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun expandNotificationPanel(): Boolean = try {
        val statusBarService = context.getSystemService("statusbar")
        val clazz = Class.forName("android.app.StatusBarManager")
        val method = clazz.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
        true
    } catch (e: Exception) {
        false
    }
}
