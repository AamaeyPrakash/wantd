package com.tapshop.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * wantd. palette. Brand white [BrandWhite] and brand black [BrandBlack] are the two anchors; everything else is
 * a tint of them. One accent, black/white pills, hairline separators.
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

val BrandWhite = Color(0xFFF6F4F0)
val BrandBlack = Color(0xFF222222)

val LightTapColors = TapColors(
    background = BrandWhite,
    surface = Color(0xFFFCFBF9),
    surfaceVariant = Color(0xFFEDEAE4),
    surfaceElevated = Color(0xFFFFFFFF),
    onSurface = BrandBlack,
    secondary = Color(0xFF6F6C68),
    tertiary = Color(0xFF9B978F),
    separator = Color(0xFFE3DFD8),
    accent = Color(0xFFFF6B3D),
    onAccent = BrandWhite,
    accentSoft = Color(0xFFFFE6DB),
    pill = BrandBlack,
    onPill = BrandWhite,
    success = Color(0xFF2FAE58),
    danger = Color(0xFFE5433A),
    scrim = Color(0x66222222),
    isDark = false,
)

val DarkTapColors = TapColors(
    background = BrandBlack,
    surface = Color(0xFF2C2C2C),
    surfaceVariant = Color(0xFF383838),
    surfaceElevated = Color(0xFF303030),
    onSurface = BrandWhite,
    secondary = Color(0xFFA9A59E),
    tertiary = Color(0xFF7C7873),
    separator = Color(0xFF3E3E3E),
    accent = Color(0xFFFF7A50),
    onAccent = BrandBlack,
    accentSoft = Color(0xFF45302A),
    pill = BrandWhite,
    onPill = BrandBlack,
    success = Color(0xFF3CCB64),
    danger = Color(0xFFFF5A50),
    scrim = Color(0x99000000),
    isDark = true,
)

val LocalTapColors = staticCompositionLocalOf { LightTapColors }
