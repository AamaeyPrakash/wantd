package com.tapshop.buyer

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.tapshop.ui.platform.readDeepLinkArticleId
import com.tapshop.ui.settings.AppSettings

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val settings = AppSettings("buyer")
    val deepLink = readDeepLinkArticleId()
    ComposeViewport {
        BuyerApp(settings = settings, deepLinkArticleId = deepLink)
    }
}
