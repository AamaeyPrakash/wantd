package com.tapshop.server

import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

object Config {
    /**
     * Settings come from environment variables first, then from a `.env` file (KEY=VALUE per line) in the
     * working directory (= repo root when started with `gradlew :server:run`) or next to the jar.
     * `.env` is git-ignored, so the OpenAI key never ends up in the repository.
     */
    private val dotEnv: Map<String, String> by lazy {
        listOf(File(".env"), File("server/.env"))
            .firstOrNull { it.isFile }
            ?.readLines()
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() && !it.startsWith("#") && '=' in it }
            ?.associate { line ->
                val key = line.substringBefore('=').trim().removePrefix("export ").trim()
                val value = line.substringAfter('=').trim().removeSurrounding("\"").removeSurrounding("'")
                key to value
            }
            ?: emptyMap()
    }

    private fun setting(name: String): String? =
        (System.getenv(name) ?: dotEnv[name])?.takeIf { it.isNotBlank() }

    val port: Int = setting("PORT")?.toIntOrNull() ?: 8080
    val openAiApiKey: String? = setting("OPENAI_API_KEY")
    val openAiModel: String? = setting("OPENAI_MODEL")
    val buyerDist: String = setting("WEB_DIST_BUYER") ?: "buyerApp/build/dist/wasmJs/productionExecutable"
    val merchantDist: String = setting("WEB_DIST_MERCHANT") ?: "merchantApp/build/dist/wasmJs/productionExecutable"
    const val VERSION = "0.1.0"

    /** Base URL encoded into QR codes. Must be reachable from the shopper's phone. */
    val publicBaseUrl: String by lazy {
        setting("PUBLIC_BASE_URL")?.trimEnd('/')
            ?: "http://${detectLanIp() ?: "localhost"}:$port"
    }

    fun detectLanIp(): String? {
        val candidates = mutableListOf<Pair<Int, String>>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (nif in interfaces) {
                if (!nif.isUp || nif.isLoopback || nif.isVirtual) continue
                val name = (nif.displayName + " " + nif.name).lowercase()
                // De-prioritise virtual adapters (Hyper-V, VirtualBox, VPNs, WSL) and prefer Wi-Fi / Ethernet.
                val penalty = when {
                    listOf("virtual", "vmware", "vbox", "hyper-v", "wsl", "vethernet", "tap", "tun", "docker", "loopback").any { it in name } -> 100
                    "wi-fi" in name || "wireless" in name || "wlan" in name -> 0
                    "ethernet" in name || "eth" in name -> 5
                    else -> 20
                }
                for (addr in nif.inetAddresses) {
                    if (addr is Inet4Address && addr.isSiteLocalAddress) {
                        candidates += penalty to addr.hostAddress
                    }
                }
            }
        } catch (_: Exception) {
        }
        return candidates.minByOrNull { it.first }?.second
    }
}
