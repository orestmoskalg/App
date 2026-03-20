package com.example.myapplication2.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AppGreen,
    onPrimary = Color.White,
    primaryContainer = AppGreenDeep,
    onPrimaryContainer = Color.White,
    secondary = AppSurfaceMuted,
    onSecondary = AppTextPrimary,
    secondaryContainer = AppSurfaceElevated,
    onSecondaryContainer = AppTextPrimary,
    tertiary = AppGreenSoft,
    onTertiary = AppTextPrimary,
    tertiaryContainer = AppSurfaceMuted,
    onTertiaryContainer = AppTextPrimary,
    background = AppBlack,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = AppTextMuted,
    outline = AppStroke,
    outlineVariant = AppSurfaceElevated,
    error = AppError,
    onError = Color.White,
)

private val LightColorScheme = lightColorScheme(
    primary = AppGreenDeep,
    onPrimary = Color.White,
    primaryContainer = AppGreenSoft,
    onPrimaryContainer = AppTextPrimary,
    secondary = AppSurfaceMuted,
    onSecondary = AppTextPrimary,
    secondaryContainer = AppSurfaceElevated,
    onSecondaryContainer = AppTextPrimary,
    tertiary = AppGreen,
    onTertiary = Color.White,
    tertiaryContainer = AppSurfaceMuted,
    onTertiaryContainer = AppTextPrimary,
    background = AppBlackSoft,
    onBackground = AppTextPrimary,
    surface = AppSurface,
    onSurface = AppTextPrimary,
    surfaceVariant = AppSurfaceMuted,
    onSurfaceVariant = AppTextMuted,
    outline = AppStroke,
    outlineVariant = AppSurfaceElevated,
    error = AppError,
    onError = Color.White,
)

@Composable
fun MyApplication2Theme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}