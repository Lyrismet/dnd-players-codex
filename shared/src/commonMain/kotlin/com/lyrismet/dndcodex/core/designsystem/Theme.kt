package com.lyrismet.dndcodex.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark Fantasy Codex is dark-only by design - no light color scheme
private val AppDarkColorScheme = darkColorScheme(
    primary = AppPalette.Gold,
    onPrimary = AppPalette.Background,
    secondary = AppPalette.Emerald,
    onSecondary = AppPalette.Background,
    tertiary = AppPalette.Azure,
    onTertiary = AppPalette.Background,
    error = AppPalette.Maroon,
    onError = AppPalette.TextPrimary,
    background = AppPalette.Background,
    onBackground = AppPalette.TextPrimary,
    surface = AppPalette.Surface,
    onSurface = AppPalette.TextPrimary,
    surfaceVariant = AppPalette.SurfaceVariant,
    onSurfaceVariant = AppPalette.TextSecondary,
    outline = AppPalette.Border,
    outlineVariant = AppPalette.BorderSubtle,
)

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppDarkColorScheme,
        typography = appTypography(),
        content = content,
    )
}
