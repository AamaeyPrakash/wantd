package com.tapshop.server

import com.tapshop.server.ai.ShoppingAssistant
import com.tapshop.server.data.InMemoryStore
import com.tapshop.server.data.Seed
import com.tapshop.server.routes.apiRoutes
import com.tapshop.server.routes.webRoutes
import com.tapshop.shared.api.AppJson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.application.log
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.routing
import org.slf4j.event.Level

fun main() {
    embeddedServer(Netty, port = Config.port, host = "0.0.0.0", module = Application::module).start(wait = true)
}

fun Application.module() {
    val store = InMemoryStore()
    Seed.apply(store)
    val assistant = ShoppingAssistant(store)

    install(ContentNegotiation) { json(AppJson) }
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.local.uri.startsWith("/api") && !call.request.local.uri.startsWith("/api/images") }
    }
    install(CORS) {
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            call.application.log.error("Unhandled error on ${call.request.local.uri}", cause)
            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (cause.message ?: cause::class.simpleName ?: "error")))
        }
    }

    routing {
        apiRoutes(store, assistant)
        webRoutes(store)
    }

    log.info("TapShop server ready")
    log.info("  Buyer app      : ${Config.publicBaseUrl}/")
    log.info("  Merchant app   : ${Config.publicBaseUrl}/merchant/")
    log.info("  QR sheet       : ${Config.publicBaseUrl}/qr-sheet")
    log.info("  AI             : ${if (assistant.mock) "DEMO MODE (set OPENAI_API_KEY to enable Koog + OpenAI)" else "Koog + OpenAI enabled"}")
}
