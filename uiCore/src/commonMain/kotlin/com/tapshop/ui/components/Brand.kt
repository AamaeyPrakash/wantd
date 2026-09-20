package com.tapshop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.res.Res
import com.tapshop.ui.res.wantd_logo
import com.tapshop.ui.theme.TapTheme
import org.jetbrains.compose.resources.painterResource

/**
 * The "wantd." wordmark. The asset is a transparent black glyph layer, so it is tinted with [tint]
 * (defaults to the theme's text colour, which flips automatically between light and dark mode).
 * Width follows the logo's aspect ratio (~3.9:1) from [height].
 */
@Composable
fun WantdLogo(
    modifier: Modifier = Modifier,
    height: Dp = 28.dp,
    tint: Color = TapTheme.colors.onSurface,
) {
    Image(
        painter = painterResource(Res.drawable.wantd_logo),
        contentDescription = "wantd.",
        contentScale = ContentScale.Fit,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.height(height),
    )
}
