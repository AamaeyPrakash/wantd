package com.tapshop.merchant

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.tapshop.merchant.screens.ArticlesScreen
import com.tapshop.merchant.screens.OverviewScreen
import com.tapshop.merchant.screens.ReservationsScreen
import com.tapshop.merchant.screens.MerchantSettingsScreen
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.app.TapShopApp
import com.tapshop.ui.components.StatusDot
import com.tapshop.ui.components.TagIcon
import com.tapshop.ui.settings.AppSettings
import com.tapshop.ui.theme.TapTheme

enum class MerchantSection { Overview, Articles, Reservations, Settings }

val LocalMerchantState = staticCompositionLocalOf<MerchantState> { error("MerchantState not provided") }

@Composable
fun MerchantApp(settings: AppSettings) {
    TapShopApp(settings = settings, toastBottomPadding = 32.dp) {
        val api = LocalApi.current
        val scope = rememberCoroutineScope()
        val state = remember(api) { MerchantState(api, scope) }
        var section by remember { mutableStateOf(MerchantSection.Overview) }

        LaunchedEffect(state) { state.startPolling() }

        CompositionLocalProvider(LocalMerchantState provides state) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val compact = maxWidth < 900.dp
                Row(Modifier.fillMaxSize()) {
                    Sidebar(section, compact = compact, onSelect = { section = it })
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        AnimatedContent(
                            targetState = section,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.fillMaxSize(),
                        ) { current ->
                            when (current) {
                                MerchantSection.Overview -> OverviewScreen()
                                MerchantSection.Articles -> ArticlesScreen()
                                MerchantSection.Reservations -> ReservationsScreen()
                                MerchantSection.Settings -> MerchantSettingsScreen()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar(selected: MerchantSection, compact: Boolean, onSelect: (MerchantSection) -> Unit) {
    val c = TapTheme.colors
    val s = TapTheme.strings
    val state = LocalMerchantState.current
    val width = if (compact) 76.dp else 240.dp

    Column(
        Modifier
            .width(width)
            .fillMaxHeight()
            .background(c.surface)
            .padding(horizontal = if (compact) 12.dp else 16.dp, vertical = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.pill), contentAlignment = Alignment.Center) {
                Icon(TagIcon, null, tint = c.onPill, modifier = Modifier.size(18.dp))
            }
            if (!compact) {
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(s.appName, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Text("Merchant", style = MaterialTheme.typography.labelSmall, color = c.secondary)
                }
            }
        }
        Spacer(Modifier.height(28.dp))

        NavItem(Icons.Rounded.Home, s.navOverview, selected == MerchantSection.Overview, compact) { onSelect(MerchantSection.Overview) }
        NavItem(Icons.AutoMirrored.Rounded.List, s.navArticles, selected == MerchantSection.Articles, compact) { onSelect(MerchantSection.Articles) }
        NavItem(
            Icons.Rounded.ShoppingCart,
            s.navReservations,
            selected == MerchantSection.Reservations,
            compact,
            badge = state.reservations.count { it.status == com.tapshop.shared.model.ReservationStatus.PENDING },
        ) { onSelect(MerchantSection.Reservations) }

        Spacer(Modifier.weight(1f))

        val info = state.serverInfo
        if (!compact && info != null) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surfaceVariant).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusDot(if (state.error == null) c.success else c.danger)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(if (state.error == null) s.live else s.offline, style = MaterialTheme.typography.labelMedium, color = c.onSurface)
                    Text(if (info.aiMock || !info.aiEnabled) s.aiStatusMock else s.aiStatusOn, style = MaterialTheme.typography.labelSmall, color = c.secondary)
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        NavItem(Icons.Rounded.Settings, s.tabSettings, selected == MerchantSection.Settings, compact) { onSelect(MerchantSection.Settings) }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, compact: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val c = TapTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) c.pill else c.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (compact) Arrangement.Center else Arrangement.Start,
    ) {
        Icon(icon, label, tint = if (selected) c.onPill else c.secondary, modifier = Modifier.size(20.dp))
        if (!compact) {
            Spacer(Modifier.width(12.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onPill else c.onSurface, modifier = Modifier.weight(1f))
            if (badge > 0) {
                Box(Modifier.size(20.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                    Text(badge.toString(), style = MaterialTheme.typography.labelSmall, color = c.onAccent)
                }
            }
        }
    }
}
