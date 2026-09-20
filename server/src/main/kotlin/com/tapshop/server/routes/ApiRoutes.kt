package com.tapshop.server.routes

import com.tapshop.server.Config
import com.tapshop.server.ai.ShoppingAssistant
import com.tapshop.server.data.Images
import com.tapshop.server.data.InMemoryStore
import com.tapshop.server.qr.QrCodes
import com.tapshop.shared.api.ApiRoutes
import com.tapshop.shared.model.AnalyticsEvent
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.ChatRequest
import com.tapshop.shared.model.CompareRequest
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.ReservationStatus
import com.tapshop.shared.model.ServerInfo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.apiRoutes(store: InMemoryStore, assistant: ShoppingAssistant) {
    route("/api") {
        get("/info") {
            call.respond(
                ServerInfo(
                    publicBaseUrl = Config.publicBaseUrl,
                    aiEnabled = !assistant.mock,
                    aiMock = assistant.mock,
                    version = Config.VERSION,
                ),
            )
        }

        // ---- Stores & articles ---------------------------------------------------------------
        get("/stores") { call.respond(store.stores()) }

        route("/articles") {
            get { call.respond(store.articles()) }
            post {
                val article = call.receive<Article>()
                call.respond(HttpStatusCode.Created, store.upsertArticle(article))
            }
            get("/{id}") {
                val article = store.article(call.parameters["id"]!!)
                if (article == null) call.respond(HttpStatusCode.NotFound, mapOf("error" to "not found"))
                else call.respond(article)
            }
            put("/{id}") {
                val id = call.parameters["id"]!!
                val body = call.receive<Article>()
                if (store.article(id) == null) {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "not found"))
                } else {
                    call.respond(store.upsertArticle(body.copy(id = id)))
                }
            }
            delete("/{id}") {
                val removed = store.deleteArticle(call.parameters["id"]!!)
                call.respond(if (removed) HttpStatusCode.NoContent else HttpStatusCode.NotFound)
            }
            get("/{id}/qr.png") {
                val id = call.parameters["id"]!!
                if (store.article(id) == null) {
                    call.respond(HttpStatusCode.NotFound)
                } else {
                    val size = call.request.queryParameters["size"]?.toIntOrNull()?.coerceIn(128, 2048) ?: 512
                    call.respondBytes(QrCodes.png(ApiRoutes.tagUrl(Config.publicBaseUrl, id), size), ContentType.Image.PNG)
                }
            }
        }

        get("/images/{file}") {
            val file = call.parameters["file"]!!
            val article = store.articles().firstOrNull { it.images.contains(file) }
            call.response.headers.append("Cache-Control", "public, max-age=86400")
            call.respondBytes(Images.bytes(file, article?.name), ContentType.parse(Images.contentType(file)))
        }

        // ---- Per-shopper state (anonymous device id) -----------------------------------------
        route("/users/{uid}") {
            route("/wishlist") {
                get { call.respond(store.wishlist(uid())) }
                post("/{articleId}") {
                    val uid = uid()
                    val articleId = call.parameters["articleId"]!!
                    val result = store.addToWishlist(uid, articleId)
                    store.record(AnalyticsEvent(uid, articleId, EventType.WISHLIST_ADD))
                    call.respond(result)
                }
                delete("/{articleId}") { call.respond(store.removeFromWishlist(uid(), call.parameters["articleId"]!!)) }
            }
            route("/cart") {
                get { call.respond(store.cart(uid())) }
                post {
                    val uid = uid()
                    val item = call.receive<CartItem>()
                    val result = store.addToCart(uid, item)
                    store.record(AnalyticsEvent(uid, item.articleId, EventType.CART_ADD))
                    call.respond(result)
                }
                put("/{articleId}") {
                    val quantity = call.request.queryParameters["quantity"]?.toIntOrNull() ?: 1
                    call.respond(store.updateCartQuantity(uid(), call.parameters["articleId"]!!, quantity))
                }
                delete("/{articleId}") { call.respond(store.removeFromCart(uid(), call.parameters["articleId"]!!)) }
            }
            post("/reserve") {
                val reservation = store.reserve(uid())
                if (reservation == null) call.respond(HttpStatusCode.BadRequest, mapOf("error" to "cart is empty"))
                else call.respond(HttpStatusCode.Created, reservation)
            }
        }

        // ---- Merchant: reservations ------------------------------------------------------------
        route("/reservations") {
            get { call.respond(store.reservations()) }
            put("/{id}") {
                val status = call.request.queryParameters["status"]?.let { runCatching { ReservationStatus.valueOf(it) }.getOrNull() }
                    ?: ReservationStatus.PICKED_UP
                val updated = store.updateReservation(call.parameters["id"]!!, status)
                if (updated == null) call.respond(HttpStatusCode.NotFound) else call.respond(updated)
            }
        }

        // ---- Analytics -------------------------------------------------------------------------
        post("/events") {
            val event = call.receive<AnalyticsEvent>()
            store.record(event)
            call.respond(HttpStatusCode.Accepted)
        }
        get("/analytics/summary") {
            val days = call.request.queryParameters["days"]?.toIntOrNull()?.coerceIn(1, 90) ?: 7
            call.respond(store.summary(days))
        }

        // ---- AI --------------------------------------------------------------------------------
        route("/ai") {
            post("/compare") {
                val request = call.receive<CompareRequest>()
                request.articleIds.forEach { store.record(AnalyticsEvent(request.uid, it, EventType.COMPARE)) }
                call.respond(assistant.compare(request))
            }
            post("/chat") {
                val request = call.receive<ChatRequest>()
                call.respond(assistant.chat(request.uid, request.messages, request.language))
            }
        }
    }
}

private fun io.ktor.server.routing.RoutingContext.uid(): String = call.parameters["uid"]!!
