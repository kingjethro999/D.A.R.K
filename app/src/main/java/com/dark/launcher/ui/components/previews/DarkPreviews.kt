package com.dark.launcher.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.dark.launcher.ui.theme.DarkTheme

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun DarkBottomBarPreview() {
    DarkTheme {
        DarkBottomBar(
            currentRoute = "home",
            steps = 1234,
            nowPlaying = com.dark.launcher.data.repo.NowPlaying(
                title = "Some Track",
                artist = "Artist",
                appName = "Spotify"
            ),
            onNavigate = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun WidgetStripPreview() {
    DarkTheme {
        WidgetStrip(
            info = com.dark.launcher.data.repo.WidgetInfo(
                batteryPercent = 78,
                batteryCharging = true,
                nextAlarm = "07:30",
                unreadCount = 3
            )
        )
    }
}
