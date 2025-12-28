package com.footprint.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)

val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// Base Colors used in default theme
val PrimaryBlue = Color(0xFF2196F3)
val SecondaryBlue = Color(0xFF03A9F4)
val TertiaryBlue = Color(0xFF00BCD4)
val AccentOrange = Color(0xFFFF9800)
val AccentTeal = Color(0xFF009688)
val AccentPurple = Color(0xFF9C27B0)
val SurfaceDark = Color(0xFF121212)

// Cyberpunk Theme (Neon & High Contrast)
val CyberpunkPrimary = Color(0xFF00FF9F) // Neon Green
val CyberpunkSecondary = Color(0xFF00B3FF) // Cyan
val CyberpunkTertiary = Color(0xFFFF0055) // Hot Pink
val CyberpunkBackground = Color(0xFF0D0208) // Deep Black

val CyberpunkDarkColors = darkColorScheme(
    primary = CyberpunkPrimary,
    secondary = CyberpunkSecondary,
    tertiary = CyberpunkTertiary,
    background = CyberpunkBackground,
    surface = Color(0xFF1A1A1A)
)

val CyberpunkLightColors = lightColorScheme(
    primary = Color(0xFF00A36C),
    secondary = Color(0xFF0077B6),
    tertiary = Color(0xFFD81B60)
)

// Forest Theme (Natural & Calm)
val ForestColors = lightColorScheme(
    primary = Color(0xFF2D5A27),
    secondary = Color(0xFF4F7942),
    tertiary = Color(0xFF8A9A5B),
    background = Color(0xFFF0F4EF),
    surface = Color.White
)

val SaharaColors = lightColorScheme(
    primary = Color(0xFFC19A6B),
    secondary = Color(0xFFE2725B),
    background = Color(0xFFF4A460)
)
