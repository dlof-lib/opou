package com.OPEN.OU.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OpouDarkColors = darkColorScheme(
    primary = OpouGradientMid,
    onPrimary = Color.White,
    secondary = OpouGradientStart,
    tertiary = OpouGradientEnd,
    background = OpouBackground,
    surface = OpouSurface,
    surfaceVariant = OpouSurfaceElevated,
    onBackground = OpouTextPrimary,
    onSurface = OpouTextPrimary,
    onSurfaceVariant = OpouTextSecondary,
    outline = OpouOutline,
    error = OpouBrokenHeart
)

private val OpouLightColors = lightColorScheme(
    primary = OpouGradientMid,
    onPrimary = Color.White,
    secondary = OpouGradientStart,
    tertiary = OpouGradientEnd,
    background = OpouBackgroundLight,
    surface = OpouSurfaceLight,
    onBackground = OpouTextPrimaryLight,
    onSurface = OpouTextPrimaryLight,
    onSurfaceVariant = OpouTextSecondaryLight,
    error = OpouBrokenHeart
)

@Composable
fun OpouTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) OpouDarkColors else OpouLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = OpouTypography,
        content = content
    )
}
