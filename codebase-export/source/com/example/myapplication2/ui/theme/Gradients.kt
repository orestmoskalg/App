package com.example.myapplication2.ui.theme

import androidx.compose.ui.graphics.Brush

val AppBackgroundGradient = Brush.verticalGradient(
    colors = listOf(
        AppBlackSoft,
        AppBlack,
        AppSurface,
    ),
)

val AppHeroGradient = Brush.linearGradient(
    colors = listOf(
        AppGreenDeep,
        AppGreen,
        AppGreenSoft,
    ),
)

val AppPanelGradient = Brush.verticalGradient(
    colors = listOf(
        AppSurfaceElevated,
        AppSurface,
    ),
)

val AppBottomBarGradient = Brush.horizontalGradient(
    colors = listOf(
        AppGreenSoft,
        AppGreen,
        AppGreenDeep,
    ),
)
