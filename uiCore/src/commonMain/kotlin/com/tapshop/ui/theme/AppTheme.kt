package com.tapshop.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tapshop.shared.i18n.AppStrings
import com.tapshop.shared.i18n.EnStrings
import com.tapshop.shared.i18n.Language

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnStrings }
val LocalLanguage = staticCompositionLocalOf { Language.EN }

object TapTheme {
    val colors: TapColors
        @Composable @ReadOnlyComposable get() = LocalTapColors.current
    val strings: AppStrings
        @Composable @ReadOnlyComposable get() = LocalStrings.current
    val language: Language
        @Composable @ReadOnlyComposable get() = LocalLanguage.current
}

val TapShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

val TapTypography = Typography(
    displayLarge = TextStyle(fontSize = 44.sp, lineHeight = 48.sp, fontWeight = FontWeight.Bold, letterSpacing = (-1.2).sp),
    displayMedium = TextStyle(fontSize = 36.sp, lineHeight = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.9).sp),
    headlineLarge = TextStyle(fontSize = 32.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
    headlineMedium = TextStyle(fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
    titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.1).sp),
    titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.Normal),
    bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Normal),
    labelLarge = TextStyle(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.6.sp),
)

@Composable
fun AppTheme(
    themeMode: ThemeMode,
    language: Language,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkTapColors else LightTapColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            secondary = colors.pill, onSecondary = colors.onPill,
            background = colors.background, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.secondary,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceVariant,
            surfaceContainerLow = colors.surface, surfaceContainerHighest = colors.surfaceVariant,
            outline = colors.separator, outlineVariant = colors.separator,
            error = colors.danger, onError = colors.onAccent,
        )
    } else {
        lightColorScheme(
            primary = colors.accent, onPrimary = colors.onAccent,
            secondary = colors.pill, onSecondary = colors.onPill,
            background = colors.background, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surfaceVariant, onSurfaceVariant = colors.secondary,
            surfaceContainer = colors.surface, surfaceContainerHigh = colors.surfaceVariant,
            surfaceContainerLow = colors.surface, surfaceContainerHighest = colors.surfaceVariant,
            outline = colors.separator, outlineVariant = colors.separator,
            error = colors.danger, onError = colors.onAccent,
        )
    }
    val direction = if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalTapColors provides colors,
        LocalStrings provides language.strings,
        LocalLanguage provides language,
        LocalLayoutDirection provides direction,
    ) {
        MaterialTheme(colorScheme = scheme, typography = TapTypography, shapes = TapShapes, content = content)
    }
}
