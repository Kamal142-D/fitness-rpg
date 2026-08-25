package com.fitnessrpg.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

/**
 * The single dark theme, assembled from [Palette]. The app is dark-first, so we
 * map the palette onto a Material 3 dark color scheme regardless of the system
 * setting.
 */
private val AppColorScheme = darkColorScheme(
    primary = Palette.Primary,
    onPrimary = Palette.Background,
    secondary = Palette.Accent,
    onSecondary = Palette.TextPrimary,
    background = Palette.Background,
    onBackground = Palette.TextPrimary,
    surface = Palette.Surface1,
    onSurface = Palette.TextPrimary,
    surfaceVariant = Palette.Surface2,
    onSurfaceVariant = Palette.TextSecondary,
    error = Palette.Danger,
    onError = Palette.TextPrimary,
    outline = Palette.HairlineStrong,
    outlineVariant = Palette.Hairline,
)

@Composable
fun FitnessRpgTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}
