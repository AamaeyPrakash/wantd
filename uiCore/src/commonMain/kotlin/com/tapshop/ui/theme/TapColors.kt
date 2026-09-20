package com.tapshop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * TapShop palette. One accent, black/white pills, hairline separators — the "designed in Cupertino" look.
 * Swap [accent] to re-skin the whole app.
 */
@Immutable
data class TapColors(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceElevated: Color,
    val onSurface: Color,
    val secondary: Color,
    val tertiary: Color,
    val separator: Color,
    val accent: Color,
    val onAccent: Color,
    val accentSoft: Color,
    val pill: Color,
    val onPill: Color,
    val success: Color,
    val danger: Color,
    val scrim: Color,
    val isDark: Boolean,
)

val LightTapColors = TapColors(
    background = Color(0xFFF4F4F6),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF2F2F5),
    surfaceElevated = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111114),
    secondary = Color(0xFF6E6E73),
    tertiary = Color(0xFF98989D),
    separator = Color(0xFFE5E5EA),
    accent = Color(0xFFFF6B3D),
    onAccent = Color(0xFFFFFFFF),
    accentSoft = Color(0xFFFFE9E1),
    pill = Color(0xFF111114),
    onPill = Color(0xFFFFFFFF),
    success = Color(0xFF34C759),
    danger = Color(0xFFFF3B30),
    scrim = Color(0x66000000),
    isDark = false,
)

val DarkTapColors = TapColors(
    background = Color(0xFF0B0B0F),
    surface = Color(0xFF1C1C1E),
    surfaceVariant = Color(0xFF2C2C2E),
    surfaceElevated = Color(0xFF232326),
    onSurface = Color(0xFFF5F5F7),
    secondary = Color(0xFF98989D),
    tertiary = Color(0xFF6E6E73),
    separator = Color(0xFF34343A),
    accent = Color(0xFFFF7A50),
    onAccent = Color(0xFF111114),
    accentSoft = Color(0xFF3A241C),
    pill = Color(0xFFF5F5F7),
    onPill = Color(0xFF111114),
    success = Color(0xFF30D158),
    danger = Color(0xFFFF453A),
    scrim = Color(0x99000000),
    isDark = true,
)

val LocalTapColors = staticCompositionLocalOf { LightTapColors }
