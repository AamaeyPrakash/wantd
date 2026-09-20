package com.tapshop.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.theme.TapTheme

/** White (or dark grey) rounded card with hairline border in dark mode. */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    padding: PaddingValues = PaddingValues(16.dp),
    onClick: (() -> Unit)? = null,
    background: Color = TapTheme.colors.surface,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = TapTheme.colors
    val shape = RoundedCornerShape(radius)
    Column(
        modifier = modifier
            .clip(shape)
            .background(background)
            .then(if (c.isDark) Modifier.border(1.dp, c.separator, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun HairlineDivider(modifier: Modifier = Modifier, inset: Dp = 0.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = inset)
            .height(1.dp)
            .background(TapTheme.colors.separator),
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = TapTheme.colors.onSurface)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = TapTheme.colors.accent,
                modifier = Modifier.clickable(enabled = onAction != null) { onAction?.invoke() },
            )
        }
    }
}

@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier, subtitle: String? = null) {
    Column(modifier) {
        Text(text, style = MaterialTheme.typography.headlineLarge, color = TapTheme.colors.onSurface)
        if (subtitle != null) {
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TapTheme.colors.secondary)
        }
    }
}

@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    val c = TapTheme.colors
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(c.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = c.secondary, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = c.onSurface, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = c.secondary, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(20.dp))
            action()
        }
    }
}

/** Label/value line used in summaries (subtotal, total…). */
@Composable
fun KeyValueRow(label: String, value: String, modifier: Modifier = Modifier, emphasized: Boolean = false, valueColor: Color? = null) {
    val c = TapTheme.colors
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = if (emphasized) c.onSurface else c.secondary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            color = valueColor ?: c.onSurface,
        )
    }
}

/** Small coloured dot (for colour swatches). */
@Composable
fun ColorDot(color: Color, selected: Boolean, size: Dp = 26.dp, onClick: (() -> Unit)? = null) {
    val c = TapTheme.colors
    Box(
        Modifier
            .size(size)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .border(if (selected) 2.dp else 1.dp, if (selected) c.onSurface else c.separator, androidx.compose.foundation.shape.CircleShape)
            .padding(if (selected) 4.dp else 2.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(color)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(Modifier.size(size).clip(androidx.compose.foundation.shape.CircleShape).background(color))
}

@Composable
fun HorizontalGap(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VerticalGap(height: Dp) = Spacer(Modifier.height(height))
