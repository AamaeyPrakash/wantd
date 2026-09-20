package com.tapshop.ui.platform

import androidx.compose.ui.graphics.ImageBitmap

/** Tiny persistent key/value store (localStorage on web, Preferences on desktop). */
expect object KeyValueStore {
    fun get(key: String): String?
    fun set(key: String, value: String)
    fun remove(key: String)
}

/** BCP-47 language tag of the host system, e.g. "fr-FR". */
expect fun systemLanguageTag(): String?

/** Random identifier for the anonymous shopper id. */
expect fun randomId(): String

/** Base URL of the TapShop API for this deployment (same origin on the served site, localhost:8080 for dev servers). */
expect fun defaultApiBaseUrl(): String

/** Article id carried by the QR / NFC deep link (`?a=<id>` or `#/a/<id>`), if the app was opened from one. */
expect fun readDeepLinkArticleId(): String?

/** Replaces the current URL query so a reload doesn't re-open the same article. */
expect fun clearDeepLink()

expect fun openUrl(url: String)

expect fun copyToClipboard(text: String)

/** Uses the native share sheet when available; returns false when the caller should fall back to copying. */
expect suspend fun shareLink(title: String, text: String, url: String): Boolean

/** Lets the user pick up to [max] images; returns them as `data:image/...;base64,...` URLs (already downscaled). */
expect suspend fun pickImagesAsDataUrls(max: Int): List<String>

/** Decodes an encoded image (JPEG/PNG/WebP) into a Compose bitmap. */
expect fun decodeImage(bytes: ByteArray): ImageBitmap?

/** Current wall-clock time in epoch millis. */
expect fun nowMillis(): Long

/** Formats an epoch-millis timestamp for the merchant UI (local time). */
expect fun formatDateTime(epochMillis: Long): String
