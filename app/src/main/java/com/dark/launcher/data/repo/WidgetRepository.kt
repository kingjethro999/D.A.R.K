package com.dark.launcher.data.repo

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WidgetInfo(
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean = false,
    val nextAlarm: String? = null,
    val unreadCount: Int = 0
)

@Singleton
class WidgetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val notifications: NotificationRepository
) {
    private val _widgets = MutableStateFlow(WidgetInfo())
    val widgets: StateFlow<WidgetInfo> = _widgets.asStateFlow()

    fun refresh() {
        _widgets.value = WidgetInfo(
            batteryPercent = batteryLevel(),
            batteryCharging = isCharging(),
            nextAlarm = nextAlarmLabel(),
            unreadCount = notifications.unreadCount.value
        )
    }

    private fun batteryLevel(): Int? {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return null
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        if (level < 0 || scale <= 0) return null
        return (level * 100 / scale)
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return false
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun nextAlarmLabel(): String? {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            ?: return null
        val info = am.nextAlarmClock ?: return null
        val time = android.text.format.DateFormat.getTimeFormat(context).format(info.triggerTime)
        return time.toString()
    }
}
