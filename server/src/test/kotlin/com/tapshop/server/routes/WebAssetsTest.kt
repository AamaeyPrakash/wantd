package com.tapshop.server.routes

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlin.test.*

class WebAssetsTest {
    private val wasmName = "0123456789abcdef0123.wasm"
    private val wasmBytes = byteArrayOf(0, 97, 115, 109, 1, 0, 0, 0)

    private fun withAssets(test: (File) -> Unit) {
        val directory = Files.createTempDirectory("wantd-web-assets-").toFile()
        try {
            directory.resolve("index.html").writeText("<html>Current app</html>")
            directory.resolve("app.js").writeText("console.log('version one')")
            directory.resolve(wasmName).writeBytes(wasmBytes)
            directory.listFiles()!!.forEach { file ->
                GZIPOutputStream(file.resolveSibling("${file.name}.gz").outputStream()).use { output ->
                    output.write(file.readBytes())
                }
            }
            test(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `buyer and merchant negotiate gzip without changing Wasm bytes or MIME type`() = withAssets { directory ->
        testApplication {
            application {
                install(ConditionalHeaders)
                routing { webApp("/", directory); webApp("/merchant", directory) }
            }
            for (prefix in listOf("", "/merchant")) {
                val compressed = client.get("$prefix/$wasmName") { header(HttpHeaders.AcceptEncoding, "gzip") }
                assertEquals(HttpStatusCode.OK, compressed.status)
                assertEquals("application/wasm", compressed.headers[HttpHeaders.ContentType])
                assertEquals("gzip", compressed.headers[HttpHeaders.ContentEncoding])
                assertContentEquals(wasmBytes, GZIPInputStream(compressed.bodyAsBytes().inputStream()).readBytes())
                assertTrue(compressed.headers[HttpHeaders.Vary].orEmpty().contains("Accept-Encoding", ignoreCase = true))
                assertTrue(compressed.headers[HttpHeaders.CacheControl].orEmpty().contains("max-age=31536000"))
                assertTrue(compressed.headers[HttpHeaders.CacheControl].orEmpty().contains("immutable"))

                for (encoding in listOf("identity", "gzip;q=0, identity;q=1", "br")) {
                    val original = client.get("$prefix/$wasmName") { header(HttpHeaders.AcceptEncoding, encoding) }
                    assertNull(original.headers[HttpHeaders.ContentEncoding], encoding)
                    assertContentEquals(wasmBytes, original.bodyAsBytes(), encoding)
                    assertTrue(original.headers[HttpHeaders.Vary].orEmpty().contains("Accept-Encoding", ignoreCase = true))
                }
            }
        }
    }

    @Test
    fun `HTML and fixed-name scripts revalidate and changed content is not returned as 304`() = withAssets { directory ->
        testApplication {
            application {
                install(ConditionalHeaders)
                routing { webApp("/", directory); webApp("/merchant", directory) }
            }
            for (path in listOf("/", "/?a=linen-overshirt", "/app.js", "/merchant/", "/merchant/app.js")) {
                val first = client.get(path) { header(HttpHeaders.AcceptEncoding, "gzip") }
                assertEquals(HttpStatusCode.OK, first.status)
                assertEquals("no-cache", first.headers[HttpHeaders.CacheControl])
                val etag = assertNotNull(first.headers[HttpHeaders.ETag])
                val cached = client.get(path) {
                    header(HttpHeaders.AcceptEncoding, "gzip")
                    header(HttpHeaders.IfNoneMatch, etag)
                }
                assertEquals(HttpStatusCode.NotModified, cached.status, path)
                assertTrue(cached.bodyAsBytes().isEmpty())
            }
            val first = client.get("/app.js") { header(HttpHeaders.AcceptEncoding, "identity") }
            val oldTag = assertNotNull(first.headers[HttpHeaders.ETag])
            // Same length and timestamp: a deployment still must change the content-based ETag.
            val script = directory.resolve("app.js")
            val modified = script.lastModified()
            script.writeText("console.log('version two')")
            script.setLastModified(modified)
            val updated = client.get("/app.js") {
                header(HttpHeaders.AcceptEncoding, "identity")
                header(HttpHeaders.IfNoneMatch, oldTag)
            }
            assertEquals(HttpStatusCode.OK, updated.status)
            assertNotEquals(oldTag, updated.headers[HttpHeaders.ETag])
            assertEquals("console.log('version two')", updated.bodyAsText())
        }
    }

    @Test
    fun `uncompressed development assets remain usable when gzip siblings are absent`() {
        val directory = Files.createTempDirectory("wantd-web-dev-").toFile()
        try {
            directory.resolve("index.html").writeText("<html>Development app</html>")
            testApplication {
                application {
                    install(ConditionalHeaders)
                    routing { webApp("/", directory) }
                }
                val response = client.get("/") { header(HttpHeaders.AcceptEncoding, "gzip") }
                assertEquals(HttpStatusCode.OK, response.status)
                assertNull(response.headers[HttpHeaders.ContentEncoding])
                assertEquals("<html>Development app</html>", response.bodyAsText())
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}
