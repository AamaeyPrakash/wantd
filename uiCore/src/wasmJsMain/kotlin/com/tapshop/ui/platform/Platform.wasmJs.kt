package com.tapshop.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.browser.localStorage
import kotlinx.browser.window
import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.skia.Image
import kotlin.js.Promise

actual object KeyValueStore {
    actual fun get(key: String): String? = try { localStorage.getItem(key) } catch (_: Throwable) { null }
    actual fun set(key: String, value: String) { try { localStorage.setItem(key, value) } catch (_: Throwable) {} }
    actual fun remove(key: String) { try { localStorage.removeItem(key) } catch (_: Throwable) {} }
}

actual fun systemLanguageTag(): String? = window.navigator.language

private fun jsRandomId(): String = js("(crypto.randomUUID ? crypto.randomUUID() : (Date.now().toString(36) + Math.random().toString(36).slice(2)))")

actual fun randomId(): String = jsRandomId().replace("-", "").take(20)

actual fun defaultApiBaseUrl(): String {
    val loc = window.location
    // Webpack dev servers run on 3000/3001 (see build.gradle.kts); the Ktor API always lives on 8080 then.
    return if (loc.port == "3000" || loc.port == "3001") "${loc.protocol}//${loc.hostname}:8080" else loc.origin
}

actual fun readDeepLinkArticleId(): String? {
    val search = window.location.search
    if (search.length > 1) {
        val params = search.removePrefix("?").split('&')
        for (p in params) {
            val (k, v) = p.split('=', limit = 2).let { it[0] to it.getOrElse(1) { "" } }
            if (k == "a" && v.isNotBlank()) return jsDecodeUri(v)
        }
    }
    val hash = window.location.hash
    if (hash.startsWith("#/a/")) return jsDecodeUri(hash.removePrefix("#/a/").substringBefore('?'))
    return null
}

private fun jsDecodeUri(value: String): String = js("decodeURIComponent(value)")

actual fun clearDeepLink() {
    try {
        window.history.replaceState(null, "", window.location.pathname)
    } catch (_: Throwable) {
    }
}

actual fun openUrl(url: String) {
    window.open(url, "_blank")
}

private fun jsCopy(text: String): Unit = js(
    """{
        if (navigator.clipboard && navigator.clipboard.writeText) { navigator.clipboard.writeText(text); }
        else {
            const ta = document.createElement('textarea'); ta.value = text; ta.style.position = 'fixed'; ta.style.opacity = '0';
            document.body.appendChild(ta); ta.select(); try { document.execCommand('copy'); } catch (e) {} document.body.removeChild(ta);
        }
    }""",
)

actual fun copyToClipboard(text: String) = jsCopy(text)

private fun jsShare(title: String, text: String, url: String): Promise<JsBoolean> = js(
    """(navigator.share
        ? navigator.share({ title: title, text: text, url: url }).then(() => true).catch(() => false)
        : Promise.resolve(false))""",
)

actual suspend fun shareLink(title: String, text: String, url: String): Boolean =
    try { jsShare(title, text, url).await<JsBoolean>().toBoolean() } catch (_: Throwable) { false }

/**
 * Opens the native file picker, downsizes each photo to max 1280px on a canvas and returns JPEG data URLs
 * (as a JSON array string) so uploads from a phone stay small.
 */
private fun jsPickImages(max: Int): Promise<JsString> = js(
    """new Promise((resolve) => {
        const input = document.createElement('input');
        input.type = 'file'; input.accept = 'image/*'; input.multiple = max > 1; input.style.display = 'none';
        document.body.appendChild(input);
        let done = false;
        const finish = (arr) => { if (done) return; done = true; try { document.body.removeChild(input); } catch (e) {} resolve(JSON.stringify(arr)); };
        const shrink = (file) => new Promise((res) => {
            const url = URL.createObjectURL(file);
            const img = new Image();
            img.onload = () => {
                const maxSide = 1280;
                const scale = Math.min(1, maxSide / Math.max(img.width, img.height));
                const canvas = document.createElement('canvas');
                canvas.width = Math.round(img.width * scale); canvas.height = Math.round(img.height * scale);
                canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
                URL.revokeObjectURL(url);
                res(canvas.toDataURL('image/jpeg', 0.85));
            };
            img.onerror = () => { URL.revokeObjectURL(url); res(null); };
            img.src = url;
        });
        input.onchange = async () => {
            const files = Array.from(input.files || []).slice(0, max);
            const out = [];
            for (const f of files) { const d = await shrink(f); if (d) out.push(d); }
            finish(out);
        };
        window.addEventListener('focus', () => setTimeout(() => { if (!done && (!input.files || input.files.length === 0)) finish([]); }, 1500), { once: true });
        input.click();
    })""",
)

actual suspend fun pickImagesAsDataUrls(max: Int): List<String> = try {
    val json = jsPickImages(max).await<JsString>().toString()
    Json.parseToJsonElement(json).jsonArray.map { it.jsonPrimitive.content }
} catch (_: Throwable) {
    emptyList()
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? = try {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (_: Throwable) {
    null
}

private fun jsNow(): Double = js("Date.now()")

actual fun nowMillis(): Long = jsNow().toLong()

private fun jsFormatDate(millis: Double): String = js("new Date(millis).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' })")

actual fun formatDateTime(epochMillis: Long): String = jsFormatDate(epochMillis.toDouble())
