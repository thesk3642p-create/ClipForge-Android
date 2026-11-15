package com.clipforge.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClipForgeColors = darkColorScheme(
    primary = Color(0xFF78E8CF),
    onPrimary = Color(0xFF08231E),
    primaryContainer = Color(0xFF183C34),
    onPrimaryContainer = Color(0xFFB9F7E9),
    secondary = Color(0xFFB9A3FF),
    onSecondary = Color(0xFF24194A),
    tertiary = Color(0xFFFFB986),
    background = Color(0xFF0B0D12),
    surface = Color(0xFF11151D),
    surfaceVariant = Color(0xFF1B202A),
    onBackground = Color(0xFFE9EDF4),
    onSurface = Color(0xFFE9EDF4),
    onSurfaceVariant = Color(0xFFA6AFBE),
    outline = Color(0xFF3C4654),
    error = Color(0xFFFF8D90),
)

@Composable
fun ClipForgeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ClipForgeColors,
        content = content,
    )
}
