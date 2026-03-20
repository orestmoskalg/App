package com.example.myapplication2.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Single source for screen rhythm: horizontal gutters, bottom inset over [NavigationBar], card geometry.
 * Use these instead of ad-hoc 12/14/20.dp values so lists and headers align across tabs.
 */
object AppDimens {
    val screenPaddingHorizontal = 16.dp
    val screenPaddingVertical = 16.dp
    /** Hero headers (title + subtitle block) — same horizontal gutter as lists */
    val headerPaddingHorizontal = 16.dp
    val headerPaddingTop = 20.dp
    val headerPaddingBottom = 16.dp
    /** Home / dashboard hero block — slightly roomier than list gutters */
    val heroBlockPadding = 24.dp

    val contentBottomInset = 100.dp
    val sectionSpacing = 8.dp
    val listItemSpacing = 10.dp

    val cardCornerRadius = 16.dp
    val cardInnerPadding = 16.dp
    val cardBorderWidth = 1.dp

    val chipRowSpacing = 8.dp
}
