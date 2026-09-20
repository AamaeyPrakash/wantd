package com.tapshop.merchant.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tapshop.merchant.LocalMerchantState
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.ReservationStatus
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalToast
import com.tapshop.ui.components.EmptyState
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.platform.formatDateTime
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

@Composable
fun ReservationsScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalMerchantState.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    val reservations = state.reservations.sortedByDescending { it.createdAt }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { PageHeader(s.navReservations, "${reservations.count { it.status == ReservationStatus.PENDING }} ${s.statusPending.lowercase()}") }
        if (reservations.isEmpty()) {
            item { EmptyState(Icons.Rounded.ShoppingCart, s.reservationsEmpty, s.cartEmptyHint) }
        } else {
            item {
                SurfaceCard(padding = PaddingValues(0.dp)) {
                    TableHeader(
                        listOf(
                            Col(s.reservationLabel, 1.2f),
                            Col(s.colShopper, 1.2f),
                            Col(s.fieldStore, 1.6f),
                            Col(s.colItems, 2.4f),
                            Col(s.colTotal, 1f, TextAlign.End),
                            Col(s.colWhen, 1.4f),
                            Col(s.colStatus, 1.6f, TextAlign.End),
                        ),
                    )
                    reservations.forEachIndexed { index, r ->
                        ReservationRow(r) {
                            scope.launch {
                                runCatching { state.markPickedUp(r.id) }
                                    .onSuccess { toast.show(s.statusPickedUp) }
                                    .onFailure { toast.show(s.errorGeneric) }
                            }
                        }
                        if (index != reservations.lastIndex) HairlineDivider(inset = 16.dp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ReservationRow(r: Reservation, onPickedUp: () -> Unit) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        TCell(r.id, 1.2f, bold = true)
        TCell(r.uid.take(8), 1.2f, color = c.secondary)
        TCell(r.storeName, 1.6f)
        Column(Modifier.weight(2.4f).padding(end = 8.dp)) {
            r.items.forEach { e ->
                val detail = listOfNotNull(e.item.size, e.item.color).joinToString(" · ")
                Text(
                    "${e.item.quantity} × ${e.article.name}" + if (detail.isNotBlank()) "  ($detail)" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.onSurface,
                    maxLines = 1,
                )
            }
        }
        TCell(formatPrice(r.totalCents, r.currency), 1f, TextAlign.End, bold = true)
        TCell(formatDateTime(r.createdAt), 1.4f, color = c.secondary)
        Box(Modifier.weight(1.6f), contentAlignment = Alignment.CenterEnd) {
            if (r.status == ReservationStatus.PENDING) {
                PillButton(s.markPickedUp, onClick = onPickedUp, style = PillStyle.Secondary, height = 36.dp, contentPadding = 14.dp)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(8.dp).height(8.dp).clip(CircleShape).background(c.success))
                    Spacer(Modifier.width(6.dp))
                    Text(s.statusPickedUp, style = MaterialTheme.typography.labelMedium, color = c.secondary)
                }
            }
        }
    }
}
