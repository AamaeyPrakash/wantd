package com.tapshop.buyer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.LocalWideLayout
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.theme.TapTheme

fun parseHexColor(hex: String, fallback: Color = Color.Gray): Color = try {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    if (clean.length == 6) Color((0xFF000000L or value).toInt()) else Color(value.toInt())
} catch (_: Throwable) {
    fallback
}

/**
 * On the desktop site, constrains a page to a readable column ([maxWidth]) centred horizontally.
 * On the phone it is a no-op so screens keep their edge-to-edge layout.
 */
@Composable
fun ReadableColumn(maxWidth: Dp = 820.dp, content: @Composable () -> Unit) {
    if (LocalWideLayout.current) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = maxWidth).fillMaxSize()) { content() }
        }
    } else {
        content()
    }
}

/** Back button + title used on pushed screens. */
@Composable
fun ScreenHeader(title: String, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    val nav = LocalNavigator.current
    val s = TapTheme.strings
    Row(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.back, onClick = { nav.pop() }, background = TapTheme.colors.surface)
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = TapTheme.colors.onSurface, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        trailing?.invoke()
    }
}

/** Portrait product card for grid layouts on the desktop site. */
@Composable
fun ProductCard(
    article: Article,
    modifier: Modifier = Modifier,
    saved: Boolean = false,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val api = LocalApi.current
    val c = TapTheme.colors
    SurfaceCard(modifier = modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
        Box {
            RemoteImage(
                url = article.images.firstOrNull()?.let(api::imageUrl),
                contentDescription = article.name,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.82f),
            )
            if (saved) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(12.dp).size(30.dp).clip(CircleShape).background(c.background.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Favorite, null, tint = c.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            Text(article.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
            Spacer(Modifier.height(2.dp))
            Text(article.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatPrice(article.priceCents, article.currency), style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                    if (subtitle != null) {
                        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (trailing != null) {
                    Spacer(Modifier.width(8.dp))
                    Box { trailing() }
                }
            }
        }
    }
}

/** Compact article row (image + name + brand + price). */
@Composable
fun ArticleRow(
    article: Article,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val api = LocalApi.current
    val c = TapTheme.colors
    SurfaceCard(modifier = modifier.fillMaxWidth(), onClick = onClick, padding = androidx.compose.foundation.layout.PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                url = article.images.firstOrNull()?.let(api::imageUrl),
                contentDescription = article.name,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(article.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
                Text(article.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle ?: formatPrice(article.priceCents, article.currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (subtitle == null) c.onSurface else c.secondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (trailing != null) {
                Spacer(Modifier.width(8.dp))
                Box { trailing() }
            }
        }
    }
}
