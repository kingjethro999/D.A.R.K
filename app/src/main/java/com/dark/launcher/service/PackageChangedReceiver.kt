package com.dark.launcher.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dark.launcher.data.repo.AppRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PackageChangedReceiver : BroadcastReceiver() {

    @Inject
    lateinit var appRepository: AppRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action?.startsWith("android.intent.action.PACKAGE_") == true) {
            appRepository.notifyAppsChanged()
        }
    }
}
