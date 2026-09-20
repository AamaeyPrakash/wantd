package com.tapshop.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class Store(
    val id: String,
    val brand: String,
    val name: String,
    val mall: String,
    val floor: String,
    val city: String,
) {
    val location: String get() = "$mall, $floor"
}

@Serializable
data class ColorOption(val name: String, val hex: String)

@Serializable
data class Article(
    val id: String,
    val storeId: String,
    val brand: String,
    val name: String,
    val category: String,
    val priceCents: Long,
    val currency: String = "USD",
    val description: String,
    val material: String,
    val care: String = "",
    val colors: List<ColorOption> = emptyList(),
    val sizes: List<String> = emptyList(),
    val images: List<String> = emptyList(),
    val stock: Int = 0,
    val tags: List<String> = emptyList(),
)

@Serializable
data class WishlistItem(
    val articleId: String,
    val storeId: String,
    val savedAt: Long,
)

@Serializable
data class WishlistEntry(
    val item: WishlistItem,
    val article: Article,
    val store: Store,
)

@Serializable
data class CartItem(
    val articleId: String,
    val size: String? = null,
    val color: String? = null,
    val quantity: Int = 1,
)

@Serializable
data class CartEntry(
    val item: CartItem,
    val article: Article,
)

@Serializable
enum class ReservationStatus { PENDING, PICKED_UP }

@Serializable
data class Reservation(
    val id: String,
    val uid: String,
    val storeId: String,
    val storeName: String,
    val items: List<CartEntry>,
    val totalCents: Long,
    val currency: String,
    val createdAt: Long,
    val status: ReservationStatus = ReservationStatus.PENDING,
)

@Serializable
enum class EventType { SCAN, VIEW, WISHLIST_ADD, CART_ADD, SHARE, COMPARE, RESERVE }

@Serializable
data class AnalyticsEvent(
    val uid: String,
    val articleId: String,
    val type: EventType,
    val timestamp: Long = 0,
)

@Serializable
data class ArticleStats(
    val articleId: String,
    val name: String,
    val brand: String,
    val image: String?,
    val scans: Int,
    val views: Int,
    val saves: Int,
    val cartAdds: Int,
    val shares: Int,
    val compares: Int,
)

@Serializable
data class DailyPoint(
    val day: String,
    val scans: Int,
    val saves: Int,
    val cartAdds: Int,
)

@Serializable
data class AnalyticsSummary(
    val totalScans: Int,
    val totalSaves: Int,
    val totalCartAdds: Int,
    val totalReservations: Int,
    val activeShoppers: Int,
    val perArticle: List<ArticleStats>,
    val daily: List<DailyPoint>,
)

@Serializable
data class CompareRequest(
    val uid: String,
    val articleIds: List<String>,
    val preferences: ComparePreferences,
    val language: String = "en",
)

@Serializable
enum class ColorPreference { ANY, NEUTRAL, DARK, LIGHT, BOLD }

@Serializable
enum class FitPreference { ANY, RELAXED, REGULAR, TAILORED, OVERSIZED }

@Serializable
enum class OccasionPreference { ANY, EVERYDAY, WORK, EVENING, TRAVEL }

@Serializable
enum class ShoppingPriority { ANY, PRICE, QUALITY, VERSATILITY, COMFORT }

@Serializable
data class ComparePreferences(
    val color: ColorPreference,
    val fit: FitPreference,
    val occasion: OccasionPreference,
    val priority: ShoppingPriority,
)

@Serializable
data class CompareResult(
    val summary: String,
    val winnerArticleId: String? = null,
    val recommendation: String = "",
    val mock: Boolean = false,
)

@Serializable
data class ChatMessage(val role: String, val content: String)

@Serializable
data class ChatRequest(
    val uid: String,
    val messages: List<ChatMessage>,
    val language: String = "en",
)

@Serializable
data class ChatResponse(val reply: String, val mock: Boolean = false)

@Serializable
data class ServerInfo(
    val publicBaseUrl: String,
    val aiEnabled: Boolean,
    val aiMock: Boolean,
    val version: String,
)

fun formatPrice(cents: Long, currency: String): String {
    val whole = cents / 100
    val frac = (cents % 100).toInt()
    val fracText = if (frac < 10) "0$frac" else "$frac"
    val symbol = when (currency) {
        "USD" -> "$"
        "EUR" -> "€"
        "GBP" -> "£"
        "AED" -> "AED "
        "JPY" -> "¥"
        else -> "$currency "
    }
    return if (currency == "JPY") "$symbol$whole" else "$symbol$whole.$fracText"
}
