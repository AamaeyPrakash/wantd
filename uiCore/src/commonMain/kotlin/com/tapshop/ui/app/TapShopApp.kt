package com.tapshop.ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.shared.api.ApiClient
import com.tapshop.ui.components.ImageLoader
import com.tapshop.ui.components.LocalImageLoader
import com.tapshop.ui.components.ToastHost
import com.tapshop.ui.components.ToastState
import com.tapshop.ui.settings.AppSettings
import com.tapshop.ui.settings.LocalAppSettings
import com.tapshop.ui.theme.AppTheme
import com.tapshop.ui.theme.TapTheme

val LocalApi = staticCompositionLocalOf<ApiClient> { error("ApiClient not provided") }
val LocalToast = staticCompositionLocalOf<ToastState> { error("ToastState not provided") }

/** Root wrapper shared by the buyer and merchant apps: theme, strings, API client, image cache, toasts. */
@Composable
fun TapShopApp(
    settings: AppSettings,
    toastBottomPadding: Dp = 96.dp,
    content: @Composable () -> Unit,
) {
    val api = remember(settings.apiBaseUrl, settings.uid) { ApiClient(settings.apiBaseUrl, settings.uid) }
    val imageLoader = remember(api) { ImageLoader { url -> api.fetchBytes(url) } }
    val toast = remember { ToastState() }

    AppTheme(themeMode = settings.themeMode, language = settings.language) {
        CompositionLocalProvider(
            LocalAppSettings provides settings,
            LocalApi provides api,
            LocalImageLoader provides imageLoader,
            LocalToast provides toast,
        ) {
            Box(Modifier.fillMaxSize().background(TapTheme.colors.background)) {
                content()
                ToastHost(toast, bottomPadding = toastBottomPadding)
            }
        }
    }
}
