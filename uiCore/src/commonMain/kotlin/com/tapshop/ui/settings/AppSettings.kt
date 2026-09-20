package com.tapshop.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import com.tapshop.shared.i18n.Language
import com.tapshop.ui.platform.KeyValueStore
import com.tapshop.ui.platform.defaultApiBaseUrl
import com.tapshop.ui.platform.randomId
import com.tapshop.ui.platform.systemLanguageTag
import com.tapshop.ui.theme.ThemeMode

/** Persisted user preferences + anonymous identity. */
class AppSettings(private val namespace: String) {
    private fun key(name: String) = "tapshop.$namespace.$name"

    var themeMode: ThemeMode by mutableStateOf(
        KeyValueStore.get(key("theme"))?.let { v -> ThemeMode.entries.firstOrNull { it.name == v } } ?: ThemeMode.SYSTEM,
    )
        private set

    var language: Language by mutableStateOf(
        KeyValueStore.get(key("language"))?.let(Language::fromCode)
            ?: Language.fromCode(systemLanguageTag())
            ?: Language.EN,
    )
        private set

    val uid: String = KeyValueStore.get(key("uid")) ?: randomId().also { KeyValueStore.set(key("uid"), it) }

    val apiBaseUrl: String = KeyValueStore.get(key("apiBaseUrl")) ?: defaultApiBaseUrl()

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        KeyValueStore.set(key("theme"), mode.name)
    }

    fun updateLanguage(lang: Language) {
        language = lang
        KeyValueStore.set(key("language"), lang.code)
    }
}

val LocalAppSettings = staticCompositionLocalOf<AppSettings> { error("AppSettings not provided") }
