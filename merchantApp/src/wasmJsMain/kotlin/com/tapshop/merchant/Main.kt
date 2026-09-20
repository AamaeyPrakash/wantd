package com.tapshop.merchant

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.tapshop.ui.settings.AppSettings

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val settings = AppSettings("merchant")
    ComposeViewport {
        MerchantApp(settings)
    }
}
