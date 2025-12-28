package com.footprint.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentOrange,
    onSecondary = Color.White,
    tertiary = AccentTeal,
    surface = Color(0xFFF4F5FB),
    onSurface = Color(0xFF0F172A)
)

private val DarkColors = darkColorScheme(
    primary = PrimaryBlue,
    onPrimary = Color.White,
    secondary = AccentOrange,
    onSecondary = Color.Black,
    tertiary = AccentTeal,
    surface = SurfaceDark,
    onSurface = Color.White
)

enum class AppThemeStyle {
    CLASSIC, CYBERPUNK, FOREST, SAHARA
}

@Composable
fun FootprintTheme(
    style: AppThemeStyle = AppThemeStyle.CLASSIC,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when (style) {
        AppThemeStyle.CYBERPUNK -> if (darkTheme) CyberpunkDarkColors else CyberpunkLightColors
        AppThemeStyle.FOREST -> ForestColors
        AppThemeStyle.SAHARA -> SaharaColors
        else -> if (darkTheme) DarkColors else LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = FootprintTypography,
        content = content
    )
}
