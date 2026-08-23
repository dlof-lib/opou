package com.OPEN.OU.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val OpouLightColors = lightColorScheme(
    primary = OpouGreen,
    onPrimary = OpouSurface,
    secondary = OpouGold,
    background = OpouBackground,
    surface = OpouSurface,
    onBackground = OpouTextPrimary,
    onSurface = OpouTextPrimary,
    error = OpouBrokenHeart
)

private val OpouDarkColors = darkColorScheme(
    primary = OpouGreenLight,
    onPrimary = OpouGreenDark,
    secondary = OpouGold,
    background = Color(0xFF0E1512),
    surface = Color(0xFF16201B),
    onBackground = Color(0xFFE7EFEA),
    onSurface = Color(0xFFE7EFEA),
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
