package com.tapshop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.theme.TapTheme

enum class PillStyle { Primary, Accent, Secondary, Ghost, Danger }

/** The signature black (or white in dark mode) rounded call-to-action. */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PillStyle = PillStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    height: Dp = 52.dp,
    contentPadding: Dp = 24.dp,
) {
    val c = TapTheme.colors
    val (bg, fg, borderColor) = when (style) {
        PillStyle.Primary -> Triple(c.pill, c.onPill, Color.Transparent)
        PillStyle.Accent -> Triple(c.accent, c.onAccent, Color.Transparent)
        PillStyle.Secondary -> Triple(c.surfaceVariant, c.onSurface, Color.Transparent)
        PillStyle.Ghost -> Triple(Color.Transparent, c.onSurface, c.separator)
        PillStyle.Danger -> Triple(c.danger.copy(alpha = 0.12f), c.danger, Color.Transparent)
    }
    val alpha = if (enabled) 1f else 0.4f
    val background by animateColorAsState(bg.copy(alpha = bg.alpha * alpha))
    val interaction = remember { MutableInteractionSource() }

    Row(
        modifier = modifier
            .height(height)
            .clip(CircleShape)
            .background(background)
            .border(if (style == PillStyle.Ghost) 1.dp else 0.dp, borderColor, CircleShape)
            .clickable(enabled = enabled && !loading, interactionSource = interaction, indication = ripple(color = fg), onClick = onClick)
            .padding(horizontal = contentPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = fg.copy(alpha = alpha), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg.copy(alpha = alpha), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = fg.copy(alpha = alpha),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Round icon button used for back / cart / share on top of imagery. */
@Composable
fun CircleIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = TapTheme.colors.surface,
    tint: Color = TapTheme.colors.onSurface,
    badge: Int = 0,
    enabled: Boolean = true,
) {
    val c = TapTheme.colors
    Box(modifier = modifier.size(size)) {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(size * 0.48f))
        }
        if (badge > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(c.accent),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (badge > 9) "9+" else badge.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.onAccent,
                )
            }
        }
    }
}

/** Small rounded chip for tags / filters. */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
) {
    val c = TapTheme.colors
    val bg = if (selected) c.pill else c.surfaceVariant
    val fg = if (selected) c.onPill else c.onSurface
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(bg)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun RowScope.Weight(weight: Float = 1f) = Spacer(Modifier.weight(weight))
