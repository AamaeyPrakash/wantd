package com.tapshop.buyer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.screens.ArticleScreen
import com.tapshop.buyer.screens.AssistantScreen
import com.tapshop.buyer.screens.CartScreen
import com.tapshop.buyer.screens.CompareScreen
import com.tapshop.buyer.screens.HomeScreen
import com.tapshop.buyer.screens.SettingsScreen
import com.tapshop.buyer.screens.WishlistScreen
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.app.TapShopApp
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.components.TagIcon
import com.tapshop.ui.nav.Navigator
import com.tapshop.ui.platform.clearDeepLink
import com.tapshop.ui.settings.AppSettings
import com.tapshop.ui.theme.TapTheme

sealed class Screen {
    data object Home : Screen()
    data object Wishlist : Screen()
    data object Cart : Screen()
    data object Assistant : Screen()
    data object Settings : Screen()
    data class Article(val id: String, val fromTag: Boolean = false) : Screen()
    data class Compare(val ids: List<String>) : Screen()

    val isTab: Boolean get() = this is Home || this is Wishlist || this is Cart || this is Assistant
}

val LocalBuyerState = staticCompositionLocalOf<BuyerState> { error("BuyerState not provided") }
val LocalNavigator = staticCompositionLocalOf<Navigator<Screen>> { error("Navigator not provided") }

/**
 * `true` when the app is rendered as a desktop website (laptop / tablet browser).
 * Screens use it to switch from the phone layout (single column, bottom tabs) to multi-column layouts.
 */
val LocalWideLayout = staticCompositionLocalOf { false }

/** Width at which the site switches from the phone layout to the desktop layout. */
val WideBreakpoint: Dp = 760.dp

/** Max content width on the desktop layout. */
val DesktopContentWidth: Dp = 1120.dp

@Composable
fun BuyerApp(settings: AppSettings, deepLinkArticleId: String?) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth >= WideBreakpoint
        TapShopApp(settings = settings, toastBottomPadding = if (wide) 32.dp else 100.dp) {
            val api = LocalApi.current
            val scope = rememberCoroutineScope()
            val state = remember(api) { BuyerState(api, scope) }
            val navigator = remember {
                Navigator<Screen>(Screen.Home).also { nav ->
                    if (deepLinkArticleId != null) nav.push(Screen.Article(deepLinkArticleId, fromTag = true))
                }
            }
            LaunchedEffect(Unit) {
                state.refresh()
                if (deepLinkArticleId != null) clearDeepLink()
            }

            CompositionLocalProvider(
                LocalBuyerState provides state,
                LocalNavigator provides navigator,
                LocalWideLayout provides wide,
            ) {
                if (wide) DesktopShell(navigator) else PhoneShell(navigator)
            }
        }
    }
}

// ─── Phone ───────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun PhoneShell(navigator: Navigator<Screen>) {
    Box(Modifier.fillMaxSize()) {
        val current = navigator.current
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                if (targetState.isTab && initialState.isTab) {
                    fadeIn() togetherWith fadeOut()
                } else if (!targetState.isTab) {
                    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (slideOutHorizontally { it / 3 } + fadeOut())
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            Box(Modifier.fillMaxSize().padding(bottom = if (screen.isTab) 78.dp else 0.dp)) {
                ScreenContent(screen)
            }
        }
        if (current.isTab) {
            TabBar(current, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun TabBar(current: Screen, modifier: Modifier = Modifier) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val nav = LocalNavigator.current
    val state = LocalBuyerState.current
    Column(modifier.fillMaxWidth().background(c.surface.copy(alpha = 0.96f))) {
        HairlineDivider()
        Row(
            Modifier.fillMaxWidth().height(77.dp).padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TabItem(Icons.Rounded.Home, s.tabHome, current is Screen.Home) { nav.reset(Screen.Home) }
            TabItem(Icons.Rounded.Favorite, s.tabWishlist, current is Screen.Wishlist, badge = state.wishlist.size) { nav.reset(Screen.Wishlist) }
            TabItem(Icons.Rounded.ShoppingCart, s.tabCart, current is Screen.Cart, badge = state.cartCount) { nav.reset(Screen.Cart) }
            TabItem(SparkleIcon, s.tabAssistant, current is Screen.Assistant) { nav.reset(Screen.Assistant) }
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val c = TapTheme.colors
    val tint = if (selected) c.onSurface else c.tertiary
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(24.dp))
            if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).padding(start = 14.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

// ─── Desktop ─────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun DesktopShell(navigator: Navigator<Screen>) {
    val c = TapTheme.colors
    Column(Modifier.fillMaxSize().background(c.background)) {
        TopNav(navigator.current)
        AnimatedContent(
            targetState = navigator.current,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            modifier = Modifier.fillMaxSize(),
        ) { screen ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = DesktopContentWidth).fillMaxSize()) {
                    ScreenContent(screen)
                }
            }
        }
    }
}

@Composable
private fun TopNav(current: Screen) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val nav = LocalNavigator.current
    val state = LocalBuyerState.current
    Column(Modifier.fillMaxWidth().background(c.surface.copy(alpha = 0.96f))) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.widthIn(max = DesktopContentWidth).fillMaxWidth().height(64.dp).padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.clip(RoundedCornerShape(12.dp)).clickable { nav.reset(Screen.Home) }.padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(c.onSurface), contentAlignment = Alignment.Center) {
                        Icon(TagIcon, null, tint = c.background, modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(s.appName, style = MaterialTheme.typography.titleMedium, color = c.onSurface, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    NavLink(Icons.Rounded.Home, s.tabHome, current is Screen.Home) { nav.reset(Screen.Home) }
                    NavLink(Icons.Rounded.Favorite, s.tabWishlist, current is Screen.Wishlist, badge = state.wishlist.size) { nav.reset(Screen.Wishlist) }
                    NavLink(Icons.Rounded.ShoppingCart, s.tabCart, current is Screen.Cart, badge = state.cartCount) { nav.reset(Screen.Cart) }
                    NavLink(SparkleIcon, s.tabAssistant, current is Screen.Assistant) { nav.reset(Screen.Assistant) }
                }
                Spacer(Modifier.width(12.dp))
                CircleIconButton(Icons.Rounded.Settings, s.tabSettings, size = 38.dp, onClick = { nav.push(Screen.Settings) })
            }
        }
        HairlineDivider()
    }
}

@Composable
private fun NavLink(icon: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val c = TapTheme.colors
    val tint = if (selected) c.onSurface else c.secondary
    Row(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.surfaceVariant else c.surface.copy(alpha = 0f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            if (badge > 0) Badge(badge, Modifier.align(Alignment.TopEnd).offset(x = 9.dp, y = (-7).dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tint, maxLines = 1)
    }
}

// ─── Shared ──────────────────────────────────────────────────────────────────────────────────────

@Composable
private fun ScreenContent(screen: Screen) {
    when (screen) {
        Screen.Home -> HomeScreen()
        Screen.Wishlist -> WishlistScreen()
        Screen.Cart -> CartScreen()
        Screen.Assistant -> AssistantScreen()
        Screen.Settings -> SettingsScreen()
        is Screen.Article -> ArticleScreen(screen.id, screen.fromTag)
        is Screen.Compare -> CompareScreen(screen.ids)
    }
}

@Composable
private fun Badge(count: Int, modifier: Modifier = Modifier) {
    val c = TapTheme.colors
    Box(modifier.size(16.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
        Text(if (count > 9) "9+" else count.toString(), style = MaterialTheme.typography.labelSmall, color = c.onAccent)
    }
}
