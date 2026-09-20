package com.tapshop.ui.platform

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Base64
import java.util.Locale
import java.util.UUID
import java.util.prefs.Preferences
import javax.imageio.ImageIO
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

private val prefs: Preferences = Preferences.userRoot().node("com/tapshop")

actual object KeyValueStore {
    actual fun get(key: String): String? = prefs.get(key, null)
    actual fun set(key: String, value: String) = prefs.put(key, value)
    actual fun remove(key: String) = prefs.remove(key)
}

actual fun systemLanguageTag(): String? = Locale.getDefault().toLanguageTag()

actual fun randomId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

actual fun defaultApiBaseUrl(): String = System.getenv("TAPSHOP_API") ?: "http://localhost:8080"

actual fun readDeepLinkArticleId(): String? = System.getProperty("tapshop.article")

actual fun clearDeepLink() { System.clearProperty("tapshop.article") }

actual fun openUrl(url: String) {
    try {
        if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI(url))
    } catch (_: Throwable) {
    }
}

actual fun copyToClipboard(text: String) {
    try {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
    } catch (_: Throwable) {
    }
}

actual suspend fun shareLink(title: String, text: String, url: String): Boolean = false

actual suspend fun pickImagesAsDataUrls(max: Int): List<String> = withContext(Dispatchers.IO) {
    val chooser = JFileChooser().apply {
        isMultiSelectionEnabled = max > 1
        fileFilter = FileNameExtensionFilter("Images", "jpg", "jpeg", "png", "webp")
        dialogTitle = "Choose photos"
    }
    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return@withContext emptyList()
    val files = if (max > 1) chooser.selectedFiles.toList() else listOfNotNull(chooser.selectedFile)
    files.take(max).mapNotNull { file ->
        try {
            val img = ImageIO.read(file) ?: return@mapNotNull null
            val scale = minOf(1.0, 1280.0 / maxOf(img.width, img.height))
            val w = (img.width * scale).toInt().coerceAtLeast(1)
            val h = (img.height * scale).toInt().coerceAtLeast(1)
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g = out.createGraphics()
            g.drawImage(img.getScaledInstance(w, h, java.awt.Image.SCALE_SMOOTH), 0, 0, null)
            g.dispose()
            val bytes = ByteArrayOutputStream().also { ImageIO.write(out, "jpg", it) }.toByteArray()
            "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            null
        }
    }
}

actual fun decodeImage(bytes: ByteArray): ImageBitmap? = try {
    Image.makeFromEncoded(bytes).toComposeImageBitmap()
} catch (_: Throwable) {
    null
}

actual fun nowMillis(): Long = System.currentTimeMillis()

private val dateFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

actual fun formatDateTime(epochMillis: Long): String =
    dateFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))
