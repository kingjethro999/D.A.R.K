package com.dark.launcher.data.model

import android.os.UserHandle

data class AppInfo(
    val name: String,
    val packageName: String,
    val isInternal: Boolean = false,
    val user: UserHandle? = null
) {
    val isDual: Boolean get() = user != null

    companion object {
        const val INTERNAL_SETTINGS = "dark.internal.settings"
        const val INTERNAL_TERMINAL = "dark.internal.terminal"
    }
}
