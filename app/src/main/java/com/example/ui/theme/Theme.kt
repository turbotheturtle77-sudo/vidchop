package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CyberColorScheme = darkColorScheme(
    primary = CyberCyan,
    onPrimary = Color.Black,
    secondary = CyberPink,
    onSecondary = Color.White,
    tertiary = CyberYellow,
    onTertiary = Color.Black,
    background = DeepSpaceObsidian,
    onBackground = TextSilver,
    surface = SurfaceSlate,
    onSurface = TextSilver,
    surfaceVariant = SurfaceTrack,
    onSurfaceVariant = TextMuted,
    error = Color(0xFFFF4D4D)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme for immersion
    dynamicColor: Boolean = false, // Disable dynamic colors to keep cyber aesthetic
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CyberColorScheme,
        typography = Typography,
        content = content
    )
}
