package com.brenoworker.androidpower.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LevelGood = Color(0xFF30A46C)
val LevelMedium = Color(0xFFF5A524)
val LevelLow = Color(0xFFE5484D)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF4ADE80),
    onPrimary = Color(0xFF00391A),
    primaryContainer = Color(0xFF14532D),
    onPrimaryContainer = Color(0xFFBBF7D0),
    secondary = Color(0xFFFBBF24),
    tertiary = Color(0xFF60A5FA),
    background = Color(0xFF101411),
    surface = Color(0xFF101411),
    surfaceVariant = Color(0xFF2A322C),
    surfaceContainer = Color(0xFF1A201B),
    surfaceContainerHigh = Color(0xFF232A24)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF15803D),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBBF7D0),
    onPrimaryContainer = Color(0xFF052E16),
    secondary = Color(0xFFB45309),
    tertiary = Color(0xFF1D4ED8),
    background = Color(0xFFFBFDF8),
    surface = Color(0xFFFBFDF8),
    surfaceVariant = Color(0xFFDDE5DB),
    surfaceContainer = Color(0xFFEFF3EC),
    surfaceContainerHigh = Color(0xFFE7ECE4)
)

@Composable
fun AndroidPowerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
