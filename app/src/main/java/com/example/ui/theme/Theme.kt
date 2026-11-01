package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = ArcCyan,
    onPrimary = Color(0xFF001F26),
    primaryContainer = Color(0xFF003644),
    onPrimaryContainer = Color(0xFFB5EFFF),
    secondary = ArcBlue,
    onSecondary = Color(0xFF001F33),
    secondaryContainer = Color(0xFF073852),
    onSecondaryContainer = Color(0xFFBDE9FF),
    tertiary = StarkGold,
    onTertiary = Color(0xFF261900),
    tertiaryContainer = Color(0xFF452E00),
    onTertiaryContainer = Color(0xFFFFDF9E),
    background = CyberBlack,
    onBackground = TextPrimary,
    surface = CyberSurface,
    onSurface = TextPrimary,
    surfaceVariant = CyberSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CyberBorder,
    outlineVariant = Color(0xFF1E293B)
)

@Composable
fun JarvisTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
