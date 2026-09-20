package com.tapshop.server.data

import java.awt.Color
import java.awt.Font
import java.awt.GradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.imageio.ImageIO

/**
 * Serves bundled product photos from `resources/images`. If a file is missing (e.g. a merchant
 * created an article without uploading a photo) a clean placeholder is rendered on the fly so
 * the UI and the AI attachments never break.
 */
object Images {
    private val cache = ConcurrentHashMap<String, ByteArray>()

    fun bytes(file: String, label: String? = null): ByteArray = cache.getOrPut(file) {
        val safe = file.substringAfterLast('/').substringAfterLast('\\')
        val stream = Images::class.java.getResourceAsStream("/images/$safe")
        stream?.use { it.readBytes() } ?: placeholder(label ?: safe.substringBeforeLast('.').replace('-', ' '))
    }

    fun exists(file: String): Boolean =
        Images::class.java.getResource("/images/${file.substringAfterLast('/')}") != null

    fun contentType(file: String): String = when (file.substringAfterLast('.').lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun placeholder(label: String): ByteArray {
        val w = 900
        val h = 1200
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        val seed = label.hashCode()
        val hue = ((seed and 0xffff) % 360) / 360f
        g.paint = GradientPaint(0f, 0f, Color.getHSBColor(hue, 0.18f, 0.96f), 0f, h.toFloat(), Color.getHSBColor(hue, 0.28f, 0.82f))
        g.fillRect(0, 0, w, h)
        g.color = Color(0, 0, 0, 40)
        g.fillRoundRect(w / 2 - 260, h / 2 - 360, 520, 720, 80, 80)
        g.color = Color(30, 30, 34)
        g.font = Font("SansSerif", Font.BOLD, 56)
        val words = label.split(' ').map { it.replaceFirstChar(Char::uppercase) }
        var y = h / 2 - 20
        for (word in words) {
            val fm = g.fontMetrics
            g.drawString(word, (w - fm.stringWidth(word)) / 2, y)
            y += fm.height
        }
        g.dispose()
        val out = ByteArrayOutputStream()
        ImageIO.write(img, "jpg", out)
        return out.toByteArray()
    }
}
