package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TerminalColorScheme = darkColorScheme(
    primary = TerminalCyan,
    onPrimary = Color.Black,
    primaryContainer = TerminalCyanBg,
    onPrimaryContainer = TerminalCyan,

    secondary = TerminalBuyGreen,
    onSecondary = Color.Black,
    secondaryContainer = TerminalBuyGreenBg,
    onSecondaryContainer = TerminalBuyGreen,

    tertiary = TerminalGold,
    onTertiary = Color.Black,

    background = TerminalBackground,
    onBackground = TerminalTextPrimary,

    surface = TerminalSurface,
    onSurface = TerminalTextPrimary,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = TerminalTextSecondary,

    outline = TerminalBorder,
    error = TerminalSellRed,
    onError = Color.White
)

@Composable
fun DayTraderTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TerminalColorScheme,
        typography = Typography,
        content = content
    )
}
