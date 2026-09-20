package com.tapshop.merchant

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.tapshop.ui.settings.AppSettings

fun main() = application {
    val settings = AppSettings("merchant")
    Window(
        onCloseRequest = ::exitApplication,
        title = "wantd. Merchant",
        state = rememberWindowState(size = DpSize(1280.dp, 840.dp), position = WindowPosition.Aligned(androidx.compose.ui.Alignment.Center)),
    ) {
        MerchantApp(settings)
    }
}
