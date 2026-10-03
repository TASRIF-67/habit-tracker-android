package com.example.habittracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = ForestLight,
    onPrimary = Color(0xFF003829),
    primaryContainer = Color(0xFF164D40),
    onPrimaryContainer = Color(0xFFD2F4DE),
    secondary = NightMuted,
    onSecondary = Color(0xFF1B342A),
    secondaryContainer = NightSurfaceVariant,
    onSecondaryContainer = NightText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF71877C),
    outlineVariant = NightOutline,
)

private val LightColors = lightColorScheme(
    primary = Forest,
    onPrimary = Color.White,
    primaryContainer = SageLight,
    onPrimaryContainer = ForestDeep,
    secondary = Sage,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6EEE5),
    onSecondaryContainer = CharcoalGreen,
    background = Cream,
    onBackground = CharcoalGreen,
    surface = WarmSurface,
    onSurface = CharcoalGreen,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = Color(0xFF59645D),
    outline = Color(0xFF7B857E),
    outlineVariant = WarmOutline,
)

@Composable
fun HabitTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = AppShapes,
        content = content,
    )
}
