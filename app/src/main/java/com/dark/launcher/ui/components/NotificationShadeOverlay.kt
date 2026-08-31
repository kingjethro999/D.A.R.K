package com.dark.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dark.launcher.data.repo.ShadeNotification
import com.dark.launcher.ui.theme.Gray
import com.dark.launcher.ui.theme.TerminalGreen

@Composable
fun NotificationShadeOverlay(
    notifications: List<ShadeNotification>,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f), RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            .clickable(onClick = onDismiss)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "> NOTIFICATIONS",
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = TerminalGreen
        )
        if (notifications.isEmpty()) {
            Text(
                text = "no recent notifications",
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = Gray
            )
        } else {
            notifications.forEach { n ->
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = n.title.ifBlank { n.packageName },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (n.text.isNotBlank()) {
                        Text(
                            text = n.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
