package com.tapshop.buyer.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapshop.ui.screens.SettingsContent
import com.tapshop.ui.settings.LocalAppSettings
import com.tapshop.ui.theme.TapTheme

@Composable
fun SettingsScreen() {
    val s = TapTheme.strings
    val settings = LocalAppSettings.current
    ReadableColumn(maxWidth = 640.dp) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ScreenHeader(s.settingsTitle)
            SettingsContent(settings, modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 32.dp))
        }
    }
}
