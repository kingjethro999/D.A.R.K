package com.dark.launcher.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.dark.launcher.MainActivity
import com.dark.launcher.service.RecordOverlayService
import com.dark.launcher.ui.navigation.DarkRoutes
import java.io.File

const val CAPCUT_PACKAGE = "com.lemon.lv"
const val CAPCUT_OVERSEAS_PACKAGE = "com.lemon.lvoverseas"
const val SAMSUNG_VIDEO_PACKAGE = "com.samsung.android.video"

fun canDrawOverlays(context: Context): Boolean = Settings.canDrawOverlays(context)

fun overlayPermissionIntent(context: Context): Intent =
    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

fun startOverlayService(context: Context) {
    val intent = Intent(context, RecordOverlayService::class.java).apply {
        action = RecordOverlayService.ACTION_START_OVERLAY
    }
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
        context.startForegroundService(intent)
    } else {
        context.startService(intent)
    }
}

fun stopOverlayService(context: Context) {
    context.stopService(Intent(context, RecordOverlayService::class.java))
}

fun isOverlayServiceRunning(): Boolean = RecordOverlayService.isRunning

fun openRecorderApp(context: Context) {
    openDarkRoute(context, DarkRoutes.RECORDER)
}

fun openDarkHome(context: Context) {
    openDarkRoute(context, DarkRoutes.HOME)
}

fun openDarkSettings(context: Context) {
    openDarkRoute(context, DarkRoutes.SETTINGS)
}

fun openDarkRoute(context: Context, route: String) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or
            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        putExtra(MainActivity.EXTRA_ROUTE, route)
    }
    runCatching { context.startActivity(intent) }
}

fun shareRecording(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "video/mp4"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(send, "Share recording")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

fun openCapCut(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val installed = runCatching {
        context.packageManager.getPackageInfo(CAPCUT_PACKAGE, 0)
    }.isSuccess
    if (installed) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            setPackage(CAPCUT_PACKAGE)
            putExtra(Intent.EXTRA_TITLE, file.name)
        }
        runCatching {
            context.startActivity(intent)
            return
        }
    }
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$CAPCUT_PACKAGE")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching { context.startActivity(market) }
}

private fun packageInstalled(context: Context, pkg: String): Boolean =
    runCatching { context.packageManager.getPackageInfo(pkg, 0) }.isSuccess

private fun videoViewIntent(uri: Uri, fileName: String, pkg: String? = null): Intent =
    Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "video/mp4")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (pkg != null) setPackage(pkg)
        putExtra(Intent.EXTRA_TITLE, fileName)
    }

private fun startVideoChooser(context: Context, uri: Uri, fileName: String, chooserTitle: String) {
    val chooser = Intent.createChooser(videoViewIntent(uri, fileName), chooserTitle)
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(chooser) }
}

/** Full editing — tries CapCut (CN or overseas) directly, then a chooser of video apps. */
fun openEdit(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    for (pkg in listOf(CAPCUT_PACKAGE, CAPCUT_OVERSEAS_PACKAGE)) {
        if (packageInstalled(context, pkg)) {
            runCatching {
                context.startActivity(videoViewIntent(uri, file.name, pkg))
                return
            }
        }
    }
    startVideoChooser(context, uri, file.name, "Edit recording")
}

/** Trimming — prefers Samsung Video's built-in trim editor, then CapCut, then a chooser. */
fun openTrim(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    if (packageInstalled(context, SAMSUNG_VIDEO_PACKAGE)) {
        runCatching {
            context.startActivity(videoViewIntent(uri, file.name, SAMSUNG_VIDEO_PACKAGE))
            return
        }
    }
    for (pkg in listOf(CAPCUT_PACKAGE, CAPCUT_OVERSEAS_PACKAGE)) {
        if (packageInstalled(context, pkg)) {
            runCatching {
                context.startActivity(videoViewIntent(uri, file.name, pkg))
                return
            }
        }
    }
    startVideoChooser(context, uri, file.name, "Trim recording")
}
