package com.tapshop.merchant.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.theme.TapTheme

/** Large page title + subtitle with an optional trailing action row. */
@Composable
fun PageHeader(title: String, subtitle: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val c = TapTheme.colors
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, color = c.onSurface)
            if (subtitle != null) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.secondary)
            }
        }
        trailing?.invoke()
    }
}

data class Col(val title: String, val weight: Float, val align: TextAlign = TextAlign.Start)

@Composable
fun TableHeader(columns: List<Col>, modifier: Modifier = Modifier) {
    val c = TapTheme.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        columns.forEach { col ->
            Text(
                col.title.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = c.secondary,
                textAlign = col.align,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(col.weight).padding(end = 8.dp),
            )
        }
    }
    HairlineDivider()
}

@Composable
fun RowScope.TCell(text: String, weight: Float, align: TextAlign = TextAlign.Start, color: Color? = null, bold: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
        color = color ?: TapTheme.colors.onSurface,
        textAlign = align,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(weight).padding(end = 8.dp),
    )
}

/** Formats a percentage like "42%". */
fun percent(numerator: Int, denominator: Int): String =
    if (denominator <= 0) "–" else "${(numerator * 100f / denominator).toInt()}%"
