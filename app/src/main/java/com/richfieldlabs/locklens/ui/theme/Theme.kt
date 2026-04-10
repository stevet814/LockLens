package com.richfieldlabs.locklens.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LockLensColors = darkColorScheme(
    primary = TealPrimary,
    onPrimary = Background,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealPrimaryContainerContent,
    secondary = AmberAccent,
    onSecondary = Background,
    secondaryContainer = AmberContainer,
    onSecondaryContainer = AmberContent,
    tertiary = TealSecondary,
    onTertiary = Background,
    tertiaryContainer = AmberContainerStrong,
    onTertiaryContainer = AmberContent,
    background = Background,
    onBackground = Foreground,
    surface = SurfaceCard,
    onSurface = Foreground,
    surfaceVariant = SurfaceCardAlt,
    surfaceContainerHigh = SurfaceCardRaised,
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
