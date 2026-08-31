package com.dark.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.dark.launcher.data.repo.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaNotificationListener : NotificationListenerService() {

    @Inject lateinit var notificationRepo: NotificationRepository

    override fun onListenerConnected() {
        super.onListenerConnected()
        pushNotifications()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        pushNotifications()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        pushNotifications()
    }

    private fun pushNotifications() {
        runCatching {
            notificationRepo.updateFromListener(activeNotifications ?: emptyArray())
        }
    }
}
