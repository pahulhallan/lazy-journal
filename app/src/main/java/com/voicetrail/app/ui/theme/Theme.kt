package com.voicetrail.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VoiceTrailLightColors = lightColorScheme(
    primary = Color(0xFF146C63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBFECE4),
    onPrimaryContainer = Color(0xFF08312D),
    secondary = Color(0xFF6D4C7D),
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFB85235),
    onTertiary = Color(0xFFFFFFFF),
    background = Color(0xFFF8FAF9),
    onBackground = Color(0xFF17211F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17211F),
    surfaceVariant = Color(0xFFE4ECE9),
    onSurfaceVariant = Color(0xFF4B5B56),
    outline = Color(0xFF74837E)
)

private val VoiceTrailShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp)
)

@Composable
fun VoiceTrailTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoiceTrailLightColors,
        shapes = VoiceTrailShapes,
        content = content
    )
}
