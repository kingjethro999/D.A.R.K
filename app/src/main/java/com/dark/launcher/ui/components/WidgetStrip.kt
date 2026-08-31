package com.dark.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dark.launcher.data.repo.WidgetInfo
import com.dark.launcher.ui.theme.Amber
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen

@Composable
fun WidgetStrip(info: WidgetInfo, modifier: Modifier = Modifier) {
    val parts = buildList {
        info.batteryPercent?.let { pct ->
            add("BAT $pct%")
        }
        info.nextAlarm?.let { add("ALARM $it") }
        if (info.unreadCount > 0) add("${info.unreadCount} UNREAD")
    }
    if (parts.isEmpty()) return
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        parts.forEach { part ->
            Text(
                text = "> $part",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                letterSpacing = 1.sp,
                color = when {
                    part.contains("UNREAD") -> Amber
                    part.contains("ALARM") -> TerminalGreen.copy(alpha = 0.7f)
                    else -> Gray
                }
            )
        }
    }
}
