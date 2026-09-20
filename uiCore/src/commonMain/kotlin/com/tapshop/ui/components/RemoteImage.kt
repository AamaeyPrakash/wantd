package com.tapshop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.tapshop.ui.platform.decodeImage
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Fetches, decodes and caches images by URL; one in-flight request per URL. */
class ImageLoader(private val fetch: suspend (String) -> ByteArray) {
    private val cache = HashMap<String, ImageBitmap>()
    private val inFlight = HashMap<String, CompletableDeferred<ImageBitmap?>>()
    private val mutex = Mutex()

    fun peek(url: String): ImageBitmap? = cache[url]

    suspend fun load(url: String): ImageBitmap? {
        cache[url]?.let { return it }
        val (deferred, owner) = mutex.withLock {
            inFlight[url]?.let { it to false } ?: (CompletableDeferred<ImageBitmap?>().also { inFlight[url] = it } to true)
        }
        if (!owner) return deferred.await()
        val bitmap = try {
            decodeImage(fetch(url))
        } catch (_: Throwable) {
            null
        }
        if (bitmap != null) cache[url] = bitmap
        mutex.withLock { inFlight.remove(url) }
        deferred.complete(bitmap)
        return bitmap
    }
}

val LocalImageLoader = staticCompositionLocalOf<ImageLoader> { error("ImageLoader not provided") }

@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
) {
    val loader = LocalImageLoader.current
    var bitmap by remember(url) { mutableStateOf(url?.let(loader::peek)) }
    LaunchedEffect(url) {
        if (url != null && bitmap == null) bitmap = loader.load(url)
    }
    Box(modifier.background(TapTheme.colors.surfaceVariant)) {
        val current = bitmap
        AnimatedVisibility(visible = current != null, enter = fadeIn()) {
            if (current != null) {
                Image(
                    bitmap = current,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = contentScale,
                    alignment = alignment,
                )
            }
        }
    }
}
