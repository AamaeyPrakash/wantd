package com.tapshop.buyer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.LocalWideLayout
import com.tapshop.buyer.Screen
import com.tapshop.shared.i18n.fmt
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.ColorOption
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.Store
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.app.LocalToast
import com.tapshop.ui.components.Chip
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.ColorDot
import com.tapshop.ui.components.EmptyState
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.QuantityStepper
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.Skeleton
import com.tapshop.ui.components.TagIcon
import com.tapshop.ui.platform.copyToClipboard
import com.tapshop.ui.platform.shareLink
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

@Composable
fun ArticleScreen(articleId: String, fromTag: Boolean) {
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    val api = LocalApi.current
    val s = TapTheme.strings
    val wide = LocalWideLayout.current

    var article by remember(articleId) { mutableStateOf(state.article(articleId)) }
    var missing by remember(articleId) { mutableStateOf(false) }
    var stores by remember { mutableStateOf<List<Store>>(emptyList()) }

    LaunchedEffect(articleId) {
        if (article == null) article = state.loadArticle(articleId)
        if (article == null) missing = true
        else {
            if (fromTag) state.track(articleId, EventType.SCAN)
            state.track(articleId, EventType.VIEW)
        }
        if (stores.isEmpty()) stores = runCatching { api.stores() }.getOrDefault(emptyList())
    }

    val a = article
    if (missing) {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader(s.appName)
            EmptyState(TagIcon, s.articleNotFound, s.homeSubtitle, action = {
                PillButton(s.back, onClick = { nav.reset(Screen.Home) })
            })
        }
        return
    }
    if (a == null) {
        if (wide) {
            Row(Modifier.fillMaxSize().padding(32.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
                Skeleton(Modifier.weight(1f).aspectRatio(0.8f), radius = 28.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Skeleton(Modifier.width(120.dp).height(14.dp))
                    Skeleton(Modifier.fillMaxWidth().height(36.dp))
                    Skeleton(Modifier.fillMaxWidth().height(120.dp))
                }
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                Skeleton(Modifier.fillMaxWidth().height(420.dp), radius = 0.dp)
                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Skeleton(Modifier.width(120.dp).height(14.dp))
                    Skeleton(Modifier.fillMaxWidth().height(28.dp))
                    Skeleton(Modifier.fillMaxWidth().height(80.dp))
                }
            }
        }
        return
    }

    val store = stores.firstOrNull { it.id == a.storeId }
    val selection = remember(a.id) { ArticleSelection(a) }

    if (wide) DesktopArticle(a, store, fromTag, selection) else PhoneArticle(a, store, fromTag, selection)
}

/** Size / colour / quantity chosen on the page. */
private class ArticleSelection(a: Article) {
    var size by mutableStateOf(a.sizes.getOrNull(a.sizes.size / 2))
    var color by mutableStateOf<ColorOption?>(a.colors.firstOrNull())
    var quantity by mutableStateOf(1)
}

// ─── Phone ───────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneArticle(a: Article, store: Store?, fromTag: Boolean, sel: ArticleSelection) {
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    val api = LocalApi.current
    val s = TapTheme.strings
    val c = TapTheme.colors

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Box(Modifier.fillMaxWidth().height(440.dp)) {
                RemoteImage(
                    url = a.images.firstOrNull()?.let(api::imageUrl),
                    contentDescription = a.name,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(
                    Modifier.fillMaxWidth().height(120.dp).align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent))),
                )
                Row(Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter), verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.back, onClick = { if (!nav.pop()) nav.reset(Screen.Home) })
                    Spacer(Modifier.weight(1f))
                    CircleIconButton(Icons.Rounded.ShoppingCart, s.tabCart, onClick = { nav.reset(Screen.Cart) }, badge = state.cartCount)
                }
                if (fromTag) {
                    ScannedChip(store?.brand ?: a.brand, Modifier.align(Alignment.BottomStart).padding(start = 20.dp, bottom = 36.dp))
                }
            }

            // Detail card overlapping the hero
            Column(
                Modifier
                    .fillMaxWidth()
                    .offset(y = (-24).dp)
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(c.background)
                    .padding(horizontal = 20.dp, vertical = 22.dp),
            ) {
                ArticleDetails(a, store, sel)
                Spacer(Modifier.height(110.dp))
            }
        }

        // Sticky action bar
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(c.background.copy(alpha = 0f), c.background, c.background)))
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ArticleActions(a, sel)
        }
    }
}

// ─── Desktop ─────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun DesktopArticle(a: Article, store: Store?, fromTag: Boolean, sel: ArticleSelection) {
    val nav = LocalNavigator.current
    val api = LocalApi.current
    val s = TapTheme.strings

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 32.dp, vertical = 24.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, s.back, background = TapTheme.colors.surface, onClick = { if (!nav.pop()) nav.reset(Screen.Home) })
            Spacer(Modifier.width(12.dp))
            Text(a.brand, style = MaterialTheme.typography.titleMedium, color = TapTheme.colors.secondary)
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Box(Modifier.weight(1f)) {
                RemoteImage(
                    url = a.images.firstOrNull()?.let(api::imageUrl),
                    contentDescription = a.name,
                    modifier = Modifier.fillMaxWidth().aspectRatio(0.8f).clip(RoundedCornerShape(28.dp)),
                )
                if (fromTag) {
                    ScannedChip(store?.brand ?: a.brand, Modifier.align(Alignment.BottomStart).padding(20.dp))
                }
            }
            Spacer(Modifier.width(48.dp))
            Column(Modifier.weight(1f).padding(top = 8.dp)) {
                ArticleDetails(a, store, sel)
                Spacer(Modifier.height(28.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ArticleActions(a, sel)
                }
            }
        }
        Spacer(Modifier.height(48.dp))
    }
}

// ─── Shared pieces ───────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScannedChip(brand: String, modifier: Modifier) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    Row(
        modifier.clip(RoundedCornerShape(999.dp)).background(c.pill.copy(alpha = 0.85f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(TagIcon, null, tint = c.onPill, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(6.dp))
        Text("${s.scannedAt} $brand", style = MaterialTheme.typography.labelSmall, color = c.onPill)
    }
}

/** Title, price, stock, size/colour/quantity pickers, composition, description and store block. */
@Composable
private fun ArticleDetails(a: Article, store: Store?, sel: ArticleSelection) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val wide = LocalWideLayout.current
    val titleStyle = if (wide) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.headlineSmall

    Row(verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(a.brand.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
            Spacer(Modifier.height(4.dp))
            Text(a.name, style = titleStyle, color = c.onSurface)
        }
        Spacer(Modifier.width(12.dp))
        Text(formatPrice(a.priceCents, a.currency), style = titleStyle, color = c.onSurface)
    }
    Spacer(Modifier.height(6.dp))
    StockLine(a)

    if (a.sizes.isNotEmpty()) {
        Spacer(Modifier.height(20.dp))
        Label(s.selectSize)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            a.sizes.forEach { size ->
                Chip(size, selected = size == sel.size, onClick = { sel.size = size })
            }
        }
    }
    if (a.colors.isNotEmpty()) {
        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Label(s.selectColor)
            sel.color?.let {
                Text("  ·  ${it.name}", style = MaterialTheme.typography.labelMedium, color = c.onSurface)
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            a.colors.forEach { option ->
                ColorDot(parseHexColor(option.hex), selected = option == sel.color, size = 30.dp) { sel.color = option }
            }
        }
    }

    Spacer(Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Label(s.quantity)
        Spacer(Modifier.weight(1f))
        QuantityStepper(sel.quantity, { sel.quantity = it }, max = a.stock.coerceAtLeast(1))
    }

    Spacer(Modifier.height(22.dp))
    HairlineDivider()
    Spacer(Modifier.height(18.dp))
    Label(s.composition)
    Spacer(Modifier.height(6.dp))
    Text(a.material, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
    if (a.care.isNotBlank()) {
        Spacer(Modifier.height(4.dp))
        Text(a.care, style = MaterialTheme.typography.bodySmall, color = c.secondary)
    }
    Spacer(Modifier.height(18.dp))
    Label(s.description)
    Spacer(Modifier.height(6.dp))
    Text(a.description, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)

    Spacer(Modifier.height(18.dp))
    Label(s.availableAt)
    Spacer(Modifier.height(8.dp))
    Row(
        Modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Place, null, tint = c.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(store?.let { "${it.brand} · ${it.name}" } ?: a.brand, style = MaterialTheme.typography.titleSmall, color = c.onSurface)
            Text(store?.let { "${it.location} · ${it.city}" } ?: "", style = MaterialTheme.typography.bodySmall, color = c.secondary)
        }
    }
    if (a.tags.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            a.tags.take(4).forEach { Chip(it) }
        }
    }
}

/** Save · Share · Add-to-cart. Must be placed inside a [Row]. */
@Composable
private fun androidx.compose.foundation.layout.RowScope.ArticleActions(a: Article, sel: ArticleSelection) {
    val state = LocalBuyerState.current
    val api = LocalApi.current
    val toast = LocalToast.current
    val s = TapTheme.strings
    val c = TapTheme.colors
    val scope = rememberCoroutineScope()

    var busySave by remember { mutableStateOf(false) }
    var busyAdd by remember { mutableStateOf(false) }
    var justAdded by remember(a.id) { mutableStateOf(false) }
    val saved = state.isSaved(a.id)

    CircleIconButton(
        if (saved) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
        s.save,
        size = 52.dp,
        tint = if (saved) c.accent else c.onSurface,
        enabled = !busySave,
        onClick = {
            scope.launch {
                busySave = true
                try {
                    val nowSaved = state.toggleWishlist(a.id)
                    toast.show(if (nowSaved) s.saved else s.remove, Icons.Rounded.Favorite)
                } catch (_: Throwable) {
                    toast.show(s.errorGeneric)
                } finally {
                    busySave = false
                }
            }
        },
    )
    CircleIconButton(Icons.Rounded.Share, s.share, size = 52.dp, onClick = {
        scope.launch {
            val base = (state.serverInfo?.publicBaseUrl ?: api.baseUrl).trimEnd('/')
            val url = "$base/?a=${a.id}"
            val shared = shareLink(a.name, s.shareText, url)
            if (!shared) {
                copyToClipboard(url)
                toast.show(s.linkCopied)
            }
            state.track(a.id, EventType.SHARE)
        }
    })
    PillButton(
        text = if (justAdded) s.addedToCart else "${s.addToCart} · ${formatPrice(a.priceCents * sel.quantity, a.currency)}",
        icon = if (justAdded) Icons.Rounded.Check else null,
        loading = busyAdd,
        enabled = a.stock > 0,
        modifier = Modifier.weight(1f),
        onClick = {
            scope.launch {
                busyAdd = true
                try {
                    state.addToCart(CartItem(a.id, sel.size, sel.color?.name, sel.quantity))
                    justAdded = true
                    toast.show(s.addedToCart, Icons.Rounded.ShoppingCart)
                } catch (_: Throwable) {
                    toast.show(s.errorGeneric)
                } finally {
                    busyAdd = false
                }
            }
        },
    )
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = TapTheme.colors.secondary)
}

@Composable
private fun StockLine(a: Article) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val (text, color) = when {
        a.stock <= 0 -> s.outOfStock to c.danger
        a.stock <= 6 -> s.lowStock.fmt(a.stock) to c.accent
        else -> s.inStock to c.success
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text("  ·  ${a.category}", style = MaterialTheme.typography.labelMedium, color = c.tertiary)
    }
}
