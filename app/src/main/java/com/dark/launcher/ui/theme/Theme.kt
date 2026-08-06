package com.dark.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = White,
    onPrimary = Black,
    primaryContainer = SurfaceHigh,
    onPrimaryContainer = White,
    secondary = DimWhite,
    onSecondary = Black,
    background = Black,
    onBackground = White,
    surface = Surface,
    onSurface = White,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Gray,
    error = Color(0xFFFF6B6B),
    onError = Black
)

@Composable
fun DarkTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = DarkTypography,
        content = content
    )
}
