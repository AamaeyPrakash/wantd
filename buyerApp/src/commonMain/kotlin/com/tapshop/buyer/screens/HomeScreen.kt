package com.tapshop.buyer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.LocalWideLayout
import com.tapshop.buyer.Screen
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.ErrorBanner
import com.tapshop.ui.components.SectionHeader
import com.tapshop.ui.components.Skeleton
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.components.TagIcon
import com.tapshop.ui.theme.TapTheme

@Composable
fun HomeScreen() {
    if (LocalWideLayout.current) DesktopHome() else PhoneHome()
}

// ─── Phone ───────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneHome() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(c.pill), contentAlignment = Alignment.Center) {
                        Icon(TagIcon, null, tint = c.onPill, modifier = Modifier.size(15.dp))
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(s.appName, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                }
                Spacer(Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Settings, s.tabSettings, onClick = { nav.push(Screen.Settings) }, size = 40.dp)
            }
        }
        item {
            Column {
                Spacer(Modifier.height(8.dp))
                Text(s.homeTitle, style = MaterialTheme.typography.displayMedium, color = c.onSurface)
                Spacer(Modifier.height(10.dp))
                Text(s.homeSubtitle, style = MaterialTheme.typography.bodyMedium, color = c.secondary)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickTiles(Modifier.weight(1f))
            }
        }
        state.error?.let { err ->
            item { ErrorBanner("${s.offline} ($err)", onRetry = { state.refresh() }, retryLabel = s.retry) }
        }
        item { SectionHeader(s.homeDiscover) }
        if (state.loading && state.articles.isEmpty()) {
            items(3) { Skeleton(Modifier.fillMaxWidth().height(96.dp), radius = 22.dp) }
        } else {
            items(state.articles, key = { it.id }) { article ->
                val store = state.wishlist.firstOrNull { it.article.id == article.id }?.store
                ArticleRow(
                    article,
                    subtitle = store?.let { "${it.brand} · ${it.location}" },
                    onClick = { nav.push(Screen.Article(article.id)) },
                    trailing = {
                        if (state.isSaved(article.id)) Icon(Icons.Rounded.Favorite, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    },
                )
            }
        }
    }
}

// ─── Desktop ─────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun DesktopHome() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 230.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 32.dp, end = 32.dp, top = 40.dp, bottom = 48.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1.25f)) {
                    Text(s.homeTitle, style = MaterialTheme.typography.displayLarge, color = c.onSurface)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        s.homeSubtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.secondary,
                        modifier = Modifier.widthIn(max = 560.dp),
                    )
                }
                Spacer(Modifier.width(40.dp))
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickTiles(Modifier.weight(1f))
                }
            }
        }
        state.error?.let { err ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                ErrorBanner("${s.offline} ($err)", onRetry = { state.refresh() }, retryLabel = s.retry)
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(s.homeDiscover, Modifier.padding(top = 16.dp))
        }
        if (state.loading && state.articles.isEmpty()) {
            items(4) { Skeleton(Modifier.fillMaxWidth().height(340.dp), radius = 22.dp) }
        } else {
            items(state.articles, key = { it.id }) { article ->
                ProductCard(
                    article,
                    saved = state.isSaved(article.id),
                    onClick = { nav.push(Screen.Article(article.id)) },
                )
            }
        }
    }
}

// ─── Shared ──────────────────────────────────────────────────────────────────────────────────────

/** The three shortcut tiles (wishlist, cart, assistant). Caller supplies the per-tile modifier (usually `weight(1f)`). */
@Composable
private fun QuickTiles(each: Modifier) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    QuickTile(Icons.Rounded.Favorite, s.tabWishlist, "${state.wishlist.size} ${s.homeSavedCount}", each, c.accentSoft, c.accent) { nav.reset(Screen.Wishlist) }
    QuickTile(Icons.Rounded.ShoppingCart, s.tabCart, state.cartCount.toString(), each, c.surfaceVariant, c.onSurface) { nav.reset(Screen.Cart) }
    QuickTile(SparkleIcon, s.tabAssistant, "AI", each, c.pill, c.onPill) { nav.reset(Screen.Assistant) }
}

@Composable
private fun QuickTile(icon: ImageVector, title: String, value: String, modifier: Modifier, bg: Color, fg: Color, onClick: () -> Unit) {
    SurfaceCard(modifier, background = bg, onClick = onClick, padding = PaddingValues(14.dp), radius = 20.dp) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(14.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = fg, maxLines = 1)
        Text(title, style = MaterialTheme.typography.labelSmall, color = fg.copy(alpha = 0.75f), maxLines = 1)
    }
}
