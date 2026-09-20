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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.LocalWideLayout
import com.tapshop.buyer.Screen
import com.tapshop.shared.model.CartEntry
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.app.LocalToast
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.EmptyState
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.KeyValueRow
import com.tapshop.ui.components.LargeTitle
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.QuantityStepper
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

@Composable
fun CartScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var reserving by remember { mutableStateOf(false) }
    var reservation by remember { mutableStateOf<Reservation?>(null) }

    val entries = state.cart
    val currency = entries.firstOrNull()?.article?.currency ?: "USD"
    val wide = LocalWideLayout.current

    val itemsList: LazyListScope.() -> Unit = {
        if (entries.isEmpty() && reservation == null) {
            item {
                EmptyState(Icons.Rounded.ShoppingCart, s.cartEmpty, s.cartEmptyHint, action = {
                    PillButton(s.tabWishlist, onClick = { nav.reset(Screen.Wishlist) }, style = PillStyle.Secondary)
                })
            }
        }
        items(entries, key = { it.article.id }) { entry ->
            CartRow(
                entry,
                onQuantity = { q -> scope.launch { runCatching { state.updateCartQuantity(entry.article.id, q) }.onFailure { toast.show(s.errorGeneric) } } },
                onRemove = { scope.launch { runCatching { state.removeFromCart(entry.article.id) }.onFailure { toast.show(s.errorGeneric) } } },
                onClick = { nav.push(Screen.Article(entry.article.id)) },
            )
        }
    }

    val summaryCard: @Composable (Modifier) -> Unit = { modifier ->
        SurfaceCard(modifier) {
            Text(s.orderSummary, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
            Spacer(Modifier.height(14.dp))
            KeyValueRow(s.subtotal, formatPrice(state.cartTotalCents, currency))
            Spacer(Modifier.height(8.dp))
            KeyValueRow(s.pickup, s.free, valueColor = c.success)
            Spacer(Modifier.height(12.dp))
            HairlineDivider()
            Spacer(Modifier.height(12.dp))
            KeyValueRow(s.total, formatPrice(state.cartTotalCents, currency), emphasized = true)
        }
    }

    val reserveButton: @Composable (Modifier) -> Unit = { modifier ->
        PillButton(
            text = "${s.reserve}  ·  ${formatPrice(state.cartTotalCents, currency)}",
            loading = reserving,
            modifier = modifier,
            onClick = {
                scope.launch {
                    reserving = true
                    try {
                        reservation = state.reserve()
                    } catch (_: Throwable) {
                        toast.show(s.errorGeneric)
                    } finally {
                        reserving = false
                    }
                }
            },
        )
    }

    Box(Modifier.fillMaxSize()) {
        if (wide) {
            Column(Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
                LargeTitle(s.cartTitle, modifier = Modifier.padding(top = 32.dp, bottom = 20.dp))
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    LazyColumn(
                        Modifier.weight(1.6f).fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) { itemsList() }
                    if (entries.isNotEmpty()) {
                        Column(Modifier.weight(1f).widthIn(max = 380.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            summaryCard(Modifier.fillMaxWidth())
                            reserveButton(Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item { LargeTitle(s.cartTitle) }
                itemsList()
                if (entries.isNotEmpty()) {
                    item { summaryCard(Modifier.padding(top = 8.dp)) }
                }
            }
            if (entries.isNotEmpty()) {
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
                    reserveButton(Modifier.fillMaxWidth())
                }
            }
        }

        reservation?.let { r ->
            Box(Modifier.fillMaxSize().background(c.scrim), contentAlignment = Alignment.Center) {
                SurfaceCard(Modifier.padding(28.dp).widthIn(max = 420.dp).fillMaxWidth(), padding = PaddingValues(24.dp), radius = 28.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(64.dp).clip(CircleShape).background(c.success.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = c.success, modifier = Modifier.size(32.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(s.reserved, style = MaterialTheme.typography.headlineSmall, color = c.onSurface)
                        Spacer(Modifier.height(6.dp))
                        Text(s.reservedHint, style = MaterialTheme.typography.bodyMedium, color = c.secondary, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(18.dp))
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceVariant).padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(s.reservationLabel.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
                            Text(r.id, style = MaterialTheme.typography.headlineMedium, color = c.onSurface)
                            Text(r.storeName, style = MaterialTheme.typography.bodySmall, color = c.secondary, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(4.dp))
                            Text(formatPrice(r.totalCents, r.currency), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                        }
                        Spacer(Modifier.height(18.dp))
                        PillButton(s.continueShopping, modifier = Modifier.fillMaxWidth(), onClick = {
                            reservation = null
                            nav.reset(Screen.Home)
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun CartRow(entry: CartEntry, onQuantity: (Int) -> Unit, onRemove: () -> Unit, onClick: () -> Unit) {
    val api = LocalApi.current
    val c = TapTheme.colors
    val s = TapTheme.strings
    val a = entry.article
    SurfaceCard(Modifier.fillMaxWidth(), padding = PaddingValues(12.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(
                url = a.images.firstOrNull()?.let(api::imageUrl),
                contentDescription = a.name,
                modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(a.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val details = listOfNotNull(entry.item.size?.let { "${s.size} $it" }, entry.item.color).joinToString("  ·  ")
                if (details.isNotBlank()) Text(details, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(formatPrice(a.priceCents * entry.item.quantity, a.currency), style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Spacer(Modifier.weight(1f))
                    QuantityStepper(entry.item.quantity, onQuantity, compact = true, max = a.stock.coerceAtLeast(1))
                }
            }
            Spacer(Modifier.width(6.dp))
            CircleIconButton(Icons.Rounded.Delete, s.remove, onClick = onRemove, size = 32.dp, background = c.surfaceVariant, tint = c.secondary)
        }
    }
}
