package com.tapshop.buyer.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Place
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
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.LocalWideLayout
import com.tapshop.buyer.Screen
import com.tapshop.shared.i18n.fmt
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.WishlistEntry
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalToast
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.EmptyState
import com.tapshop.ui.components.LargeTitle
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

@Composable
fun WishlistScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()

    var selecting by remember { mutableStateOf(false) }
    val selected = remember { mutableStateOf(setOf<String>()) }
    val entries = state.wishlist
    val grouped = entries.groupBy { it.store.id }

    fun toggle(id: String) {
        selected.value = if (id in selected.value) selected.value - id else if (selected.value.size < 2) selected.value + id else selected.value
    }

    val wide = LocalWideLayout.current
    val fullRow: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = if (wide) GridCells.Adaptive(minSize = 340.dp) else GridCells.Fixed(1),
            modifier = Modifier.fillMaxSize(),
            contentPadding = if (wide) PaddingValues(start = 32.dp, end = 32.dp, top = 32.dp, bottom = 120.dp)
            else PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(span = fullRow) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LargeTitle(s.wishlistTitle, subtitle = if (entries.isEmpty()) null else s.itemsCount.fmt(entries.size), modifier = Modifier.weight(1f))
                    if (entries.size >= 2) {
                        Text(
                            if (selecting) s.cancel else s.select,
                            style = MaterialTheme.typography.labelLarge,
                            color = c.accent,
                            modifier = Modifier.clickable {
                                selecting = !selecting
                                selected.value = emptySet()
                            }.padding(8.dp),
                        )
                    }
                }
            }
            if (selecting) {
                item(span = fullRow) {
                    Text(s.selectAtLeastTwo, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                }
            }
            if (entries.isEmpty()) {
                item(span = fullRow) {
                    EmptyState(Icons.Rounded.FavoriteBorder, s.wishlistEmpty, s.wishlistEmptyHint, action = {
                        PillButton(s.homeDiscover, onClick = { nav.reset(Screen.Home) }, style = PillStyle.Secondary)
                    })
                }
            }
            grouped.forEach { (_, group) ->
                val store = group.first().store
                item(key = "store-${store.id}", span = fullRow) {
                    Row(Modifier.padding(top = 8.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Place, null, tint = c.accent, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${store.brand}  ·  ${store.location}", style = MaterialTheme.typography.labelMedium, color = c.secondary)
                    }
                }
                items(group, key = { it.article.id }) { entry ->
                    WishlistRow(
                        entry = entry,
                        selecting = selecting,
                        selected = entry.article.id in selected.value,
                        onClick = { if (selecting) toggle(entry.article.id) else nav.push(Screen.Article(entry.article.id)) },
                        onRemove = {
                            scope.launch {
                                runCatching { state.removeFromWishlist(entry.article.id) }.onFailure { toast.show(s.errorGeneric) }
                            }
                        },
                        onMoveToCart = {
                            scope.launch {
                                runCatching {
                                    state.addToCart(CartItem(entry.article.id, entry.article.sizes.getOrNull(entry.article.sizes.size / 2), entry.article.colors.firstOrNull()?.name, 1))
                                    toast.show(s.addedToCart, Icons.Rounded.ShoppingCart)
                                }.onFailure { toast.show(s.errorGeneric) }
                            }
                        },
                    )
                }
            }
        }

        // Floating compare pill
        AnimatedVisibility(
            visible = entries.size >= 2,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp),
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
        ) {
            val selectedIds = entries.map { it.article.id }.filter { it in selected.value }
            val count = selectedIds.size
            val label = if (selecting) "${s.compareWithAi} ($count/2)" else s.compareWithAi
            PillButton(
                text = label,
                icon = SparkleIcon,
                style = PillStyle.Primary,
                enabled = !selecting || count == 2,
                onClick = {
                    if (!selecting) {
                        selecting = true
                        selected.value = emptySet()
                    } else if (count == 2) {
                        nav.push(Screen.Compare(selectedIds))
                    } else {
                        toast.show(s.selectAtLeastTwo)
                    }
                },
            )
        }
    }
}

@Composable
private fun WishlistRow(
    entry: WishlistEntry,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveToCart: () -> Unit,
) {
    val c = TapTheme.colors
    val s = TapTheme.strings
    val a = entry.article
    Box(
        Modifier.fillMaxWidth().then(
            if (selecting && selected) Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp)).border(2.dp, c.accent, androidx.compose.foundation.shape.RoundedCornerShape(22.dp)) else Modifier,
        ),
    ) {
        ArticleRow(
            article = a,
            subtitle = formatPrice(a.priceCents, a.currency),
            onClick = onClick,
            trailing = {
                if (selecting) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape)
                            .background(if (selected) c.accent else c.surfaceVariant)
                            .border(1.dp, if (selected) c.accent else c.separator, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (selected) Icon(Icons.Rounded.Check, null, tint = c.onAccent, modifier = Modifier.size(16.dp))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CircleIconButton(Icons.Rounded.ShoppingCart, s.moveToCart, onClick = onMoveToCart, size = 34.dp, background = c.surfaceVariant)
                        CircleIconButton(Icons.Rounded.Delete, s.remove, onClick = onRemove, size = 34.dp, background = c.surfaceVariant, tint = c.secondary)
                    }
                }
            },
        )
    }
}
