package com.richfieldlabs.locklens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LockLensColors = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Background,
    secondary = TealSecondary,
    tertiary = TealTertiary,
    background = Background,
    onBackground = Foreground,
    surface = SurfaceCard,
    onSurface = Foreground,
    surfaceVariant = SurfaceCardAlt,
    onSurfaceVariant = ForegroundMuted,
    outline = Outline,
    error = ErrorRed,
)

@Composable
fun LockLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LockLensColors,
        typography = LockLensTypography,
        content = content,
    )
}
