import java.util.zip.GZIPOutputStream

// Generate compressed siblings as part of the existing production distribution.
// Keep the original files for browsers that do not accept gzip.
val webDistribution = layout.buildDirectory.dir("dist/wasmJs/productionExecutable")
tasks.named("wasmJsBrowserDistribution") {
    doLast {
        val destinationDir = webDistribution.get().asFile
        // Discover the emitted names on every build; never pin a previous build's Wasm hash.
        val index = destinationDir.resolve("index.html")
        val preloadBlock = destinationDir.listFiles().orEmpty()
            .filter { it.isFile && it.extension == "wasm" }
            .sortedBy { it.name }
            .joinToString("\n    ") {
                """<link rel="preload" href="${it.name}" as="fetch" type="application/wasm" crossorigin>"""
            }
        val html = index.readText().replace(
            Regex("<!-- wantd-wasm-preloads -->[\\s\\S]*?<!-- /wantd-wasm-preloads -->\\s*"), "",
        )
        index.writeText(html.replaceFirst("<script", "<!-- wantd-wasm-preloads -->\n    $preloadBlock\n    <!-- /wantd-wasm-preloads -->\n    <script"))

        destinationDir.walkTopDown()
            .filter { it.isFile && it.extension in setOf("wasm", "js", "mjs", "css", "html", "json", "svg") }
            .forEach { asset ->
                GZIPOutputStream(asset.resolveSibling("${asset.name}.gz").outputStream().buffered()).use { output ->
                    asset.inputStream().buffered().use { input -> input.copyTo(output) }
                }
            }
    }
}
