package com.dark.launcher.data.repo

import android.service.notification.StatusBarNotification
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ShadeNotification(
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long
)

@Singleton
class NotificationRepository @Inject constructor() {

    private val _recent = MutableStateFlow<List<ShadeNotification>>(emptyList())
    val recent: StateFlow<List<ShadeNotification>> = _recent.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    fun updateFromListener(notifications: Array<StatusBarNotification>) {
        val items = notifications
            .filter { it.notification.extras.getCharSequence(android.app.Notification.EXTRA_TITLE) != null }
            .sortedByDescending { it.postTime }
            .take(5)
            .map { sbn ->
                val extras = sbn.notification.extras
                ShadeNotification(
                    packageName = sbn.packageName,
                    title = extras.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString().orEmpty(),
                    text = extras.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString().orEmpty(),
                    timestamp = sbn.postTime
                )
            }
        _recent.value = items
        _unreadCount.value = notifications.count { it.isClearable && !it.isOngoing }
    }
}
