package com.dark.launcher.util

import android.app.admin.DevicePolicyManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import android.provider.Settings
import com.dark.launcher.data.model.AppInfo
import com.dark.launcher.service.DarkDeviceAdminReceiver
import com.dark.launcher.service.LauncherAccessibilityService

fun launchApp(context: Context, packageName: String) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(launchIntent)
}

fun launchApp(context: Context, app: AppInfo) {
    val user = app.user
    if (user != null && user != Process.myUserHandle()) {
        val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activity = runCatching { launcherApps.getActivityList(app.packageName, user) }
            .getOrDefault(emptyList())
            .firstOrNull()
        if (activity != null) {
            val launched = runCatching {
                launcherApps.startMainActivity(activity.componentName, user, null, null)
            }.isSuccess
            if (launched) return
        }
    }
    launchApp(context, app.packageName)
}

fun openAppInfo(context: Context, packageName: String) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun uninstallApp(context: Context, packageName: String) {
    val intent = Intent(Intent.ACTION_DELETE).apply {
        data = Uri.parse("package:$packageName")
    }
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun shareApp(context: Context, app: AppInfo) {
    val link = "https://play.google.com/store/apps/details?id=${app.packageName}"
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out this app: $link")
    }
    val chooser = Intent.createChooser(send, "Share ${app.name} via")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

fun requestLockScreen(context: Context) {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    val admin = ComponentName(context, DarkDeviceAdminReceiver::class.java)
    if (dpm.isAdminActive(admin)) {
        runCatching { dpm.lockNow() }
        return
    }
    val intent = Intent(LauncherAccessibilityService.ACTION_LOCK_SCREEN).apply {
        setPackage(context.packageName)
    }
    context.sendBroadcast(intent)
}

fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isAdminActive(ComponentName(context, DarkDeviceAdminReceiver::class.java))
}

fun isLockAccessibilityEnabled(context: Context): Boolean {
    val expected = ComponentName(context, LauncherAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    val splitter = android.text.TextUtils.SimpleStringSplitter(':')
    splitter.setString(enabled)
    while (splitter.hasNext()) {
        val name = ComponentName.unflattenFromString(splitter.next())
        if (name == expected) return true
    }
    return false
}

fun isLockReady(context: Context): Boolean =
    isDeviceAdminActive(context) || isLockAccessibilityEnabled(context)

fun deviceAdminEnableIntent(context: Context): Intent =
    Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
        putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, ComponentName(context, DarkDeviceAdminReceiver::class.java))
        putExtra(
            DevicePolicyManager.EXTRA_ADD_EXPLANATION,
            "D.A.R.K. uses device admin to lock the screen instantly from home gestures and the lock command."
        )
    }
