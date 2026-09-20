package com.tapshop.shared.api

import com.tapshop.shared.model.AnalyticsEvent
import com.tapshop.shared.model.AnalyticsSummary
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.CartEntry
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.ChatRequest
import com.tapshop.shared.model.ChatResponse
import com.tapshop.shared.model.CompareRequest
import com.tapshop.shared.model.CompareResult
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.ReservationStatus
import com.tapshop.shared.model.ServerInfo
import com.tapshop.shared.model.Store
import com.tapshop.shared.model.WishlistEntry
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsBytes
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    explicitNulls = false
}

class ApiClient(
    val baseUrl: String,
    val uid: String,
) {
    private val http = HttpClient {
        expectSuccess = true
        install(ContentNegotiation) { json(AppJson) }
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 15_000
        }
    }

    private fun url(path: String) = baseUrl.trimEnd('/') + path

    fun imageUrl(file: String): String =
        if (file.startsWith("http")) file else url(ApiRoutes.image(file))

    fun qrUrl(articleId: String): String = url(ApiRoutes.articleQr(articleId))
    fun qrSheetUrl(): String = url(ApiRoutes.QR_SHEET)

    suspend fun info(): ServerInfo = http.get(url(ApiRoutes.INFO)).body()

    suspend fun articles(): List<Article> = http.get(url(ApiRoutes.ARTICLES)).body()
    suspend fun article(id: String): Article = http.get(url(ApiRoutes.article(id))).body()
    suspend fun createArticle(article: Article): Article =
        http.post(url(ApiRoutes.ARTICLES)) { contentType(ContentType.Application.Json); setBody(article) }.body()
    suspend fun updateArticle(article: Article): Article =
        http.put(url(ApiRoutes.article(article.id))) { contentType(ContentType.Application.Json); setBody(article) }.body()
    suspend fun deleteArticle(id: String) { http.delete(url(ApiRoutes.article(id))) }

    suspend fun stores(): List<Store> = http.get(url(ApiRoutes.STORES)).body()

    suspend fun wishlist(): List<WishlistEntry> = http.get(url(ApiRoutes.wishlist(uid))).body()
    suspend fun addToWishlist(articleId: String): List<WishlistEntry> =
        http.post(url(ApiRoutes.wishlistItem(uid, articleId))).body()
    suspend fun removeFromWishlist(articleId: String): List<WishlistEntry> =
        http.delete(url(ApiRoutes.wishlistItem(uid, articleId))).body()

    suspend fun cart(): List<CartEntry> = http.get(url(ApiRoutes.cart(uid))).body()
    suspend fun addToCart(item: CartItem): List<CartEntry> =
        http.post(url(ApiRoutes.cart(uid))) { contentType(ContentType.Application.Json); setBody(item) }.body()
    suspend fun updateCartQuantity(articleId: String, quantity: Int): List<CartEntry> =
        http.put(url(ApiRoutes.cartItem(uid, articleId))) { parameter("quantity", quantity) }.body()
    suspend fun removeFromCart(articleId: String): List<CartEntry> =
        http.delete(url(ApiRoutes.cartItem(uid, articleId))).body()
    suspend fun reserve(): Reservation = http.post(url(ApiRoutes.reserve(uid))).body()

    suspend fun reservations(): List<Reservation> = http.get(url(ApiRoutes.RESERVATIONS)).body()
    suspend fun updateReservation(id: String, status: ReservationStatus): Reservation =
        http.put(url(ApiRoutes.reservation(id))) { parameter("status", status.name) }.body()

    /** Fire-and-forget analytics; failures are swallowed so they never break the UI. */
    suspend fun track(articleId: String, type: EventType) {
        try {
            http.post(url(ApiRoutes.EVENTS)) {
                contentType(ContentType.Application.Json)
                setBody(AnalyticsEvent(uid = uid, articleId = articleId, type = type))
            }
        } catch (_: Throwable) {
        }
    }

    suspend fun analyticsSummary(): AnalyticsSummary = http.get(url(ApiRoutes.ANALYTICS_SUMMARY)).body()

    suspend fun compare(request: CompareRequest): CompareResult =
        http.post(url(ApiRoutes.AI_COMPARE)) { contentType(ContentType.Application.Json); setBody(request) }.body()

    suspend fun chat(request: ChatRequest): ChatResponse =
        http.post(url(ApiRoutes.AI_CHAT)) { contentType(ContentType.Application.Json); setBody(request) }.body()

    suspend fun fetchBytes(fullUrl: String): ByteArray {
        val response: HttpResponse = http.get(fullUrl)
        return response.bodyAsBytes()
    }
}
