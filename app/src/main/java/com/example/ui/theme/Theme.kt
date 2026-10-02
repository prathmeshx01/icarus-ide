package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

val IcarusShapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(12.dp)
)

private val ZincColorScheme = darkColorScheme(
    primary = ZincAccent,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E2D4A),
    onPrimaryContainer = Color(0xFFC8E1FF),
    secondary = ZincAccentCyan,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF13383B),
    onSecondaryContainer = Color(0xFFA5F3FC),
    tertiary = ZincPurple,
    background = ZincBackground,
    onBackground = ZincTextPrimary,
    surface = ZincSurface,
    onSurface = ZincTextPrimary,
    surfaceVariant = ZincSurfaceVariant,
    onSurfaceVariant = ZincTextSecondary,
    outline = ZincBorder,
    outlineVariant = ZincBorderLight,
    error = ZincRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ZincColorScheme,
        typography = Typography,
        shapes = IcarusShapes,
        content = content
    )
}
