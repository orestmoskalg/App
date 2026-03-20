package com.example.myapplication2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ══════════════════════════════════════════════════════════════════
//  Brand + base: logo teals on an overall white / off-white UI.
//  Priority / criticality: red → orange → teal → blue-grey (LOW).
// ══════════════════════════════════════════════════════════════════

// ── Logo (reference) ─────────────────────────────────────────────
// Deep teal #159185 — primary accent; light teal #33C9B3 — secondary highlights

val LogoTealDark = Color(0xFF159185)
val LogoTealLight = Color(0xFF33C9B3)

// ── Base (mostly white, soft teal wash) ──────────────────────────

val AccentTealMain = LogoTealDark           // Buttons, tabs, icons, links
val LightTealBg = Color(0xFFE8F4F2)         // Selected chip / nav pill (very light teal)
val AppBackground = Color(0xFFF8FBFA)      // Screen bg: white with a hint of teal
val PureWhite = Color(0xFFFFFFFF)           // Cards, bars, surfaces
val PrimaryTextDark = Color(0xFF212121)     // Primary text: headings, titles
val SecondaryTextMedium = Color(0xFF616161)  // Secondary text: subtitles, dates, inactive
val TertiaryGray = Color(0xFF9E9E9E)        // Muted text: event count, secondary labels
val BorderGray = Color(0xFFE0E0E0)          // Borders, dividers, past-event date block bg
val BorderSubtle = Color(0xFFEEEEEE)        // Very light borders

// ── Event / card priority (CRITICAL → HIGH → MEDIUM → LOW) ───────
val PriorityCriticalColor = Color(0xFFC62828) // Red 800 — must-act deadlines
val PriorityHighColor = Color(0xFFEF6C00)       // Orange 800 — high attention
val PriorityMediumColor = LogoTealDark          // Aligned with brand teal
val PriorityLowColor = Color(0xFF546E7A)      // Blue grey 700 — informational
val PriorityLowSurface = Color(0xFFECEFF1)    // Blue grey 50 — LOW badge background

val CriticalRed = PriorityCriticalColor
val AppError = PriorityCriticalColor
val MediumPriorityGreen = PriorityMediumColor
val FutureEventOrange = PriorityHighColor
val SuccessGreen = LogoTealLight            // Positive chips, icons, stat accents
val AppGreenDark = Color(0xFF0A4F4C)        // Dark theme: deeper teal container
val MidnightBlue = Color(0xFFFFFFFF)       // Dark theme bg (background, surface) — white
val SystemNavBarBackground = AppBackground  // Keep system bar light; brand on in-app bars
val CardShadow = Color(0x1A000000)          // Card shadow (semi-transparent black)

// ── Derived (do not change — they use the colours above) ────────────
val AppGreen = AccentTealMain
val AppGreenLight = LightTealBg
val PrimaryGreen = AccentTealMain
val PrimaryTeal = AccentTealMain
val ForestGreen = AccentTealMain
val DarkGreen = MediumPriorityGreen
val MidGreen = AccentTealMain
val AccentGreen = AccentTealMain
val LightGreen = LightTealBg
val SoftGreen = LightTealBg
val PaleGreen = LightTealBg
val PaleTeal = LightTealBg
val LightTeal = LightTealBg
val DarkTeal = AppGreenDark
val MidTeal = AccentTealMain
val MintWhite = PureWhite
val OffWhite = PureWhite
val CardBg = PureWhite
val CardBgWhite = PureWhite
val ContentBg = AppBackground
val SearchBarBg = PureWhite
val SearchBarIcon = PrimaryTextDark
val TabUnselected = SecondaryTextMedium
val TabSelected = AccentTealMain
val TabIndicator = AccentTealMain
val AccuracyBg = LogoTealDark             // Solid badge: keep white label legible
val AccuracyText = PureWhite
val TagTealBg = Color(0xFFE3F5F2)           // Slightly stronger than LightTealBg for tags
val TagTealText = LogoTealDark
val RequiredRed = AppError
val RequiredBg = AppError.copy(alpha = 0.08f)
val RequiredBorder = AppError.copy(alpha = 0.3f)
val LinkIconGrey = SecondaryTextMedium
val BottomNavBg = PureWhite
val BottomNavUnsel = SecondaryTextMedium
val BottomNavSelBg = LightTealBg
val BottomNavSelIcon = AccentTealMain
val BorderGreen = BorderGray
val WarningAmber = FutureEventOrange
val ErrorRed = CriticalRed
val CrimsonRed = CriticalRed
val EmeraldGreen = AccentTealMain
val AmberOrange = FutureEventOrange
val LightAmber = FutureEventOrange
val PaleAmber = LightTealBg
val LightRed = CriticalRed
val PaleRed = RequiredBg
val InfoBlue = AccentTealMain
val TextOnDark = PureWhite
val TextMutedDark = TertiaryGray
val TextOnLight = PrimaryTextDark
val TextMutedLight = SecondaryTextMedium
val TextSubtle = TertiaryGray
val AppBlack = PrimaryTextDark
val AppGray = SecondaryTextMedium
val AppGrayLight = TertiaryGray
val NavyBlue = AppGreenDark
val AccentBlue = AccentTealMain
val SurfaceDark = MidnightBlue
val CardDark = AccentTealMain
val BorderDark = AccentTealMain
val TextPrimary = TextOnDark
val TextSecondary = TextMutedDark

// Calendar (derived): date block bg — past / today / critical / future
val CalendarPastDateBg = BorderGray
val CalendarCurrentDateBg = AccentTealMain
val CalendarCriticalDateBg = PriorityCriticalColor
/** Distant-future date pill (not urgent window) — light amber, dark text */
val CalendarFutureDateBg = Color(0xFFFFE0B2)

// ── Color Schemes ──────────────────────────────────────────────────
private val DarkColors = darkColorScheme(
    primary = LogoTealLight,
    onPrimary = Color(0xFF06312E),
    primaryContainer = AppGreenDark,
    onPrimaryContainer = LightTealBg,
    secondary = LogoTealLight,
    onSecondary = Color(0xFF06312E),
    tertiary = LogoTealLight,
    onTertiary = Color(0xFF06312E),
    background = MidnightBlue,
    onBackground = PrimaryTextDark,
    surface = MidnightBlue,
    onSurface = PrimaryTextDark,
    surfaceVariant = Color(0xFFE8F4F2),
    onSurfaceVariant = PrimaryTextDark,
    outline = Color(0xFFB8DDD7),
    error = CriticalRed,
    onError = PureWhite,
)

private val LightColors = lightColorScheme(
    primary = AccentTealMain,
    onPrimary = PureWhite,
    primaryContainer = LightTealBg,
    onPrimaryContainer = Color(0xFF0D4A47),
    secondary = LogoTealLight,
    onSecondary = Color(0xFF063D3A),
    tertiary = LogoTealLight,
    onTertiary = PureWhite,
    background = AppBackground,
    onBackground = PrimaryTextDark,
    surface = PureWhite,
    onSurface = PrimaryTextDark,
    surfaceVariant = Color(0xFFF2F9F8),
    onSurfaceVariant = PrimaryTextDark,
    outline = Color(0xFFC5E8E2),
    error = CriticalRed,
    onError = PureWhite,
)

// ── Typography ─────────────────────────────────────────────────────
val AppTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 40.sp),
    displayMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 34.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, lineHeight = 30.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, lineHeight = 26.sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 24.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 15.sp, lineHeight = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 10.sp, lineHeight = 14.sp),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
