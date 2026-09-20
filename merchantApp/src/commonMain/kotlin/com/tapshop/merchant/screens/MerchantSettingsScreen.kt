package com.tapshop.merchant.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tapshop.merchant.LocalMerchantState
import com.tapshop.ui.screens.SettingsContent
import com.tapshop.ui.settings.LocalAppSettings
import com.tapshop.ui.theme.TapTheme

@Composable
fun MerchantSettingsScreen() {
    val s = TapTheme.strings
    val settings = LocalAppSettings.current
    val info = LocalMerchantState.current.serverInfo
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item { PageHeader(s.settingsTitle) }
        item {
            SettingsContent(
                settings,
                modifier = Modifier.widthIn(max = 640.dp),
                extraAbout = buildList {
                    if (info != null) {
                        add(s.tagUrl to info.publicBaseUrl)
                        add("AI" to if (info.aiMock || !info.aiEnabled) s.aiStatusMock else s.aiStatusOn)
                    }
                },
            )
        }
    }
}
