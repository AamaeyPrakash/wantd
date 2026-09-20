package com.tapshop.server.routes

import com.tapshop.server.Config
import com.tapshop.server.data.InMemoryStore
import com.tapshop.shared.api.ApiRoutes
import com.tapshop.shared.model.formatPrice
import io.ktor.http.ContentType
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

/** Serves the two Compose-for-Web apps and the printable QR sheet. */
fun Route.webRoutes(store: InMemoryStore) {
    get(ApiRoutes.QR_SHEET) {
        call.respondText(qrSheetHtml(store), ContentType.Text.Html)
    }

    val buyerDir = resolveDist(Config.buyerDist)
    val merchantDir = resolveDist(Config.merchantDist)

    if (merchantDir != null) {
        staticFiles("/merchant", merchantDir) {
            default("index.html")
            contentType(::webContentType)
        }
    }
    if (buyerDir != null) {
        staticFiles("/", buyerDir) {
            default("index.html")
            contentType(::webContentType)
        }
    } else {
        get("/") {
            call.respondText(
                """
                <html><body style="font-family:-apple-system,Segoe UI,sans-serif;padding:40px;color:#111">
                <h2>TapShop server is running</h2>
                <p>The buyer web app has not been built yet. Run:</p>
                <pre>gradlew :buyerApp:wasmJsBrowserDistribution :merchantApp:wasmJsBrowserDistribution</pre>
                <p>and restart the server, or use the dev servers (see README).</p>
                <p><a href="${ApiRoutes.QR_SHEET}">QR sheet</a> · <a href="/api/articles">API</a></p>
                </body></html>
                """.trimIndent(),
                ContentType.Text.Html,
            )
        }
    }
}

private fun webContentType(file: File): ContentType = when (file.extension.lowercase()) {
    "wasm" -> ContentType.parse("application/wasm")
    "js", "mjs" -> ContentType.Text.JavaScript
    "html" -> ContentType.Text.Html
    "css" -> ContentType.Text.CSS
    "json" -> ContentType.Application.Json
    "png" -> ContentType.Image.PNG
    "svg" -> ContentType.Image.SVG
    "ico" -> ContentType.parse("image/x-icon")
    "woff2" -> ContentType.parse("font/woff2")
    "woff" -> ContentType.parse("font/woff")
    "ttf" -> ContentType.parse("font/ttf")
    else -> ContentType.Application.OctetStream
}

/** Prefer the production build; fall back to the development one so a quick dev build still serves. */
private fun resolveDist(path: String): File? {
    val prod = File(path)
    if (File(prod, "index.html").exists()) return prod
    val dev = File(path.replace("productionExecutable", "developmentExecutable"))
    if (File(dev, "index.html").exists()) return dev
    return null
}

private fun qrSheetHtml(store: InMemoryStore): String {
    val base = Config.publicBaseUrl
    val cards = store.articles().joinToString("\n") { a ->
        val s = store.store(a.storeId)
        """
        <div class="card">
          <img class="qr" src="${ApiRoutes.articleQr(a.id)}?size=600" alt="QR ${a.name}">
          <div class="meta">
            <div class="brand">${a.brand}</div>
            <div class="name">${a.name}</div>
            <div class="price">${formatPrice(a.priceCents, a.currency)}</div>
            <div class="store">${s?.let { "${it.name} · ${it.location}" } ?: ""}</div>
            <div class="hint">Tap the NFC tag or scan to save this piece</div>
            <div class="url">${ApiRoutes.tagUrl(base, a.id)}</div>
          </div>
        </div>
        """.trimIndent()
    }
    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
      <meta charset="utf-8">
      <title>TapShop · QR sheet</title>
      <style>
        :root { color-scheme: light; }
        body { margin: 0; padding: 32px; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Inter, Roboto, sans-serif; background: #F4F4F6; color: #111114; }
        h1 { font-size: 28px; letter-spacing: -0.02em; margin: 0 0 4px; }
        .sub { color: #6E6E73; margin-bottom: 24px; font-size: 14px; }
        .grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 24px; }
        .card { background: #fff; border-radius: 24px; padding: 24px; display: flex; gap: 20px; align-items: center; box-shadow: 0 1px 2px rgba(0,0,0,.04); page-break-inside: avoid; }
        .qr { width: 220px; height: 220px; border-radius: 16px; background: #fff; }
        .brand { font-size: 12px; text-transform: uppercase; letter-spacing: .12em; color: #6E6E73; }
        .name { font-size: 22px; font-weight: 600; letter-spacing: -0.01em; margin: 2px 0; }
        .price { font-size: 18px; margin-bottom: 6px; }
        .store { color: #6E6E73; font-size: 13px; }
        .hint { margin-top: 10px; font-size: 13px; }
        .url { margin-top: 6px; font-size: 11px; color: #98989D; word-break: break-all; }
        .toolbar { display: flex; gap: 12px; align-items: center; margin-bottom: 24px; }
        button { border: 0; background: #111114; color: #fff; padding: 12px 20px; border-radius: 999px; font-size: 14px; cursor: pointer; }
        a { color: #111114; }
        @media print { body { background: #fff; padding: 0; } .toolbar, .sub a { display: none; } .card { box-shadow: none; border: 1px solid #eee; } }
      </style>
    </head>
    <body>
      <h1>TapShop tags</h1>
      <div class="sub">Print and place on the racks. Shoppers open <b>$base</b> · Merchant dashboard: <a href="$base/merchant/">$base/merchant/</a></div>
      <div class="toolbar"><button onclick="window.print()">Print</button><span style="color:#6E6E73;font-size:13px">${store.articles().size} pieces</span></div>
      <div class="grid">
      $cards
      </div>
    </body>
    </html>
    """.trimIndent()
}
