package com.tapshop.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.theme.TapTheme

/** iOS-style segmented control with a sliding thumb. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 40.dp,
) {
    val c = TapTheme.colors
    if (options.isEmpty()) return
    BoxWithConstraints(
        modifier
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(c.surfaceVariant)
            .padding(3.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val offset by animateDpAsState(segmentWidth * selectedIndex.coerceIn(0, options.lastIndex), spring(stiffness = 600f))
        Box(
            Modifier
                .offset(x = offset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(10.dp))
                .background(c.surface),
        )
        Row(Modifier.fillMaxWidth().fillMaxHeight()) {
            options.forEachIndexed { index, label ->
                val selected = index == selectedIndex
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelected(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium),
                        color = if (selected) c.onSurface else c.secondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/** "- 1 +" stepper. */
@Composable
fun QuantityStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 99,
    compact: Boolean = false,
) {
    val c = TapTheme.colors
    val size = if (compact) 28.dp else 34.dp
    Row(
        modifier
            .clip(CircleShape)
            .background(c.surfaceVariant)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepperButton(MinusIcon, enabled = value > min, size = size) { onChange(value - 1) }
        Text(
            value.toString(),
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.titleMedium,
            color = c.onSurface,
            modifier = Modifier.width(if (compact) 28.dp else 36.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        StepperButton(Icons.Rounded.Add, enabled = value < max, size = size) { onChange(value + 1) }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    val c = TapTheme.colors
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(c.surface)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = c.onSurface.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(size * 0.5f))
    }
}

/** Material icons core has no "Remove"; a one-line minus glyph. */
val MinusIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Minus", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(5f, 11f); lineTo(19f, 11f); lineTo(19f, 13f); lineTo(5f, 13f); close()
            }
        }.build()
}

/** Four-point sparkle used for the AI assistant. */
val SparkleIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Sparkle", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(12f, 2f); curveTo(12.6f, 7.2f, 16.8f, 11.4f, 22f, 12f)
                curveTo(16.8f, 12.6f, 12.6f, 16.8f, 12f, 22f)
                curveTo(11.4f, 16.8f, 7.2f, 12.6f, 2f, 12f)
                curveTo(7.2f, 11.4f, 11.4f, 7.2f, 12f, 2f); close()
            }
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(19f, 1f); curveTo(19.2f, 2.6f, 20.4f, 3.8f, 22f, 4f)
                curveTo(20.4f, 4.2f, 19.2f, 5.4f, 19f, 7f)
                curveTo(18.8f, 5.4f, 17.6f, 4.2f, 16f, 4f)
                curveTo(17.6f, 3.8f, 18.8f, 2.6f, 19f, 1f); close()
            }
        }.build()
}

/** Rounded "tag" glyph used for the scan explainer. */
val TagIcon: ImageVector by lazy {
    ImageVector.Builder(name = "Tag", defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(fill = androidx.compose.ui.graphics.SolidColor(Color.Black)) {
                moveTo(3f, 4f); lineTo(11f, 4f); lineTo(21f, 14f); lineTo(13f, 22f); lineTo(3f, 12f); close()
                moveTo(5f, 6f); lineTo(5f, 11.2f); lineTo(13f, 19.2f); lineTo(18.2f, 14f); lineTo(10.2f, 6f); close()
                moveTo(8f, 7.5f); curveTo(8.8f, 7.5f, 9.5f, 8.2f, 9.5f, 9f); curveTo(9.5f, 9.8f, 8.8f, 10.5f, 8f, 10.5f)
                curveTo(7.2f, 10.5f, 6.5f, 9.8f, 6.5f, 9f); curveTo(6.5f, 8.2f, 7.2f, 7.5f, 8f, 7.5f); close()
            }
        }.build()
}

/** Text field styled like an iOS grouped input. */
@Composable
fun TapTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    trailing: (@Composable () -> Unit)? = null,
    onSubmit: (() -> Unit)? = null,
) {
    val c = TapTheme.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = c.tertiary) } },
        singleLine = singleLine,
        minLines = minLines,
        trailingIcon = trailing,
        shape = RoundedCornerShape(14.dp),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { onSubmit?.invoke() }, onDone = { onSubmit?.invoke() }),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            imeAction = if (onSubmit != null) androidx.compose.ui.text.input.ImeAction.Send else androidx.compose.ui.text.input.ImeAction.Default,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.onSurface.copy(alpha = 0.5f),
            unfocusedBorderColor = c.separator,
            focusedContainerColor = c.surface,
            unfocusedContainerColor = c.surface,
            focusedTextColor = c.onSurface,
            unfocusedTextColor = c.onSurface,
            focusedLabelColor = c.secondary,
            unfocusedLabelColor = c.secondary,
            cursorColor = c.accent,
        ),
    )
}
