package com.tapshop.server.data

import com.tapshop.shared.model.AnalyticsEvent
import com.tapshop.shared.model.AnalyticsSummary
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ArticleStats
import com.tapshop.shared.model.CartEntry
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.DailyPoint
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.ReservationStatus
import com.tapshop.shared.model.Store
import com.tapshop.shared.model.WishlistEntry
import com.tapshop.shared.model.WishlistItem
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

/**
 * Thread-safe in-memory data store. Everything lives in one process for the demo;
 * swapping this for a database only touches this file.
 */
class InMemoryStore {
    private val lock = Any()

    private val stores = LinkedHashMap<String, Store>()
    private val articles = LinkedHashMap<String, Article>()
    private val wishlists = HashMap<String, MutableList<WishlistItem>>()
    private val carts = HashMap<String, MutableList<CartItem>>()
    private val reservations = LinkedHashMap<String, Reservation>()
    private val events = ArrayList<AnalyticsEvent>()

    // ---- Stores & articles -------------------------------------------------------------------

    fun stores(): List<Store> = synchronized(lock) { stores.values.toList() }
    fun store(id: String): Store? = synchronized(lock) { stores[id] }
    fun putStore(store: Store) = synchronized(lock) { stores[store.id] = store }

    fun articles(): List<Article> = synchronized(lock) { articles.values.toList() }
    fun article(id: String): Article? = synchronized(lock) { articles[id] }

    fun upsertArticle(article: Article): Article = synchronized(lock) {
        val withId = if (article.id.isBlank()) article.copy(id = newArticleId(article.name)) else article
        articles[withId.id] = withId
        withId
    }

    fun deleteArticle(id: String): Boolean = synchronized(lock) {
        val removed = articles.remove(id) != null
        if (removed) {
            wishlists.values.forEach { list -> list.removeAll { it.articleId == id } }
            carts.values.forEach { list -> list.removeAll { it.articleId == id } }
        }
        removed
    }

    private fun newArticleId(name: String): String {
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').take(24).ifBlank { "article" }
        var candidate = slug
        var i = 2
        while (articles.containsKey(candidate)) candidate = "$slug-${i++}"
        return candidate
    }

    // ---- Wishlist --------------------------------------------------------------------------------

    fun wishlist(uid: String): List<WishlistEntry> = synchronized(lock) {
        wishlists[uid].orEmpty().mapNotNull { item ->
            val article = articles[item.articleId] ?: return@mapNotNull null
            val store = stores[item.storeId] ?: stores[article.storeId] ?: return@mapNotNull null
            WishlistEntry(item, article, store)
        }.sortedByDescending { it.item.savedAt }
    }

    fun addToWishlist(uid: String, articleId: String): List<WishlistEntry> {
        synchronized(lock) {
            val article = articles[articleId] ?: return wishlist(uid)
            val list = wishlists.getOrPut(uid) { mutableListOf() }
            if (list.none { it.articleId == articleId }) {
                list += WishlistItem(articleId = articleId, storeId = article.storeId, savedAt = System.currentTimeMillis())
            }
        }
        return wishlist(uid)
    }

    fun removeFromWishlist(uid: String, articleId: String): List<WishlistEntry> {
        synchronized(lock) { wishlists[uid]?.removeAll { it.articleId == articleId } }
        return wishlist(uid)
    }

    // ---- Cart ------------------------------------------------------------------------------------

    fun cart(uid: String): List<CartEntry> = synchronized(lock) {
        carts[uid].orEmpty().mapNotNull { item ->
            val article = articles[item.articleId] ?: return@mapNotNull null
            CartEntry(item, article)
        }
    }

    fun addToCart(uid: String, item: CartItem): List<CartEntry> {
        synchronized(lock) {
            if (!articles.containsKey(item.articleId)) return cart(uid)
            val list = carts.getOrPut(uid) { mutableListOf() }
            val existing = list.indexOfFirst { it.articleId == item.articleId }
            if (existing >= 0) {
                val current = list[existing]
                list[existing] = current.copy(
                    quantity = current.quantity + item.quantity.coerceAtLeast(1),
                    size = item.size ?: current.size,
                    color = item.color ?: current.color,
                )
            } else {
                list += item.copy(quantity = item.quantity.coerceAtLeast(1))
            }
        }
        return cart(uid)
    }

    fun updateCartQuantity(uid: String, articleId: String, quantity: Int): List<CartEntry> {
        synchronized(lock) {
            val list = carts[uid] ?: return cart(uid)
            val idx = list.indexOfFirst { it.articleId == articleId }
            if (idx >= 0) {
                if (quantity <= 0) list.removeAt(idx) else list[idx] = list[idx].copy(quantity = quantity)
            }
        }
        return cart(uid)
    }

    fun removeFromCart(uid: String, articleId: String): List<CartEntry> {
        synchronized(lock) { carts[uid]?.removeAll { it.articleId == articleId } }
        return cart(uid)
    }

    fun reserve(uid: String): Reservation? {
        val entries = cart(uid)
        if (entries.isEmpty()) return null
        val storeIds = entries.map { it.article.storeId }.distinct()
        val storeName = if (storeIds.size == 1) {
            store(storeIds.first())?.let { "${it.brand} · ${it.name}" } ?: storeIds.first()
        } else {
            storeIds.mapNotNull { store(it)?.brand }.joinToString(", ")
        }
        val reservation = Reservation(
            id = "R-" + UUID.randomUUID().toString().take(6).uppercase(),
            uid = uid,
            storeId = storeIds.first(),
            storeName = storeName,
            items = entries,
            totalCents = entries.sumOf { it.article.priceCents * it.item.quantity },
            currency = entries.first().article.currency,
            createdAt = System.currentTimeMillis(),
        )
        synchronized(lock) {
            reservations[reservation.id] = reservation
            carts[uid]?.clear()
        }
        entries.forEach { record(AnalyticsEvent(uid, it.article.id, EventType.RESERVE, reservation.createdAt)) }
        return reservation
    }

    fun reservations(): List<Reservation> = synchronized(lock) { reservations.values.sortedByDescending { it.createdAt } }

    fun updateReservation(id: String, status: ReservationStatus): Reservation? = synchronized(lock) {
        val existing = reservations[id] ?: return null
        val updated = existing.copy(status = status)
        reservations[id] = updated
        updated
    }

    // ---- Analytics -----------------------------------------------------------------------------

    fun record(event: AnalyticsEvent) {
        val stamped = if (event.timestamp == 0L) event.copy(timestamp = System.currentTimeMillis()) else event
        synchronized(lock) {
            if (articles.containsKey(stamped.articleId)) events += stamped
        }
    }

    fun summary(days: Int = 7, zone: ZoneId = ZoneId.systemDefault()): AnalyticsSummary = synchronized(lock) {
        val now = System.currentTimeMillis()
        val since = now - days * 24L * 3600_000L
        val recent = events.filter { it.timestamp >= since }

        val perArticle = articles.values.map { a ->
            val e = recent.filter { it.articleId == a.id }
            ArticleStats(
                articleId = a.id,
                name = a.name,
                brand = a.brand,
                image = a.images.firstOrNull(),
                scans = e.count { it.type == EventType.SCAN },
                views = e.count { it.type == EventType.VIEW },
                saves = e.count { it.type == EventType.WISHLIST_ADD },
                cartAdds = e.count { it.type == EventType.CART_ADD },
                shares = e.count { it.type == EventType.SHARE },
                compares = e.count { it.type == EventType.COMPARE },
            )
        }.sortedByDescending { it.scans }

        val today = LocalDate.now(zone)
        val daily = (days - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val dayEvents = recent.filter { Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate() == day }
            DailyPoint(
                day = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                scans = dayEvents.count { it.type == EventType.SCAN },
                saves = dayEvents.count { it.type == EventType.WISHLIST_ADD },
                cartAdds = dayEvents.count { it.type == EventType.CART_ADD },
            )
        }

        AnalyticsSummary(
            totalScans = perArticle.sumOf { it.scans },
            totalSaves = perArticle.sumOf { it.saves },
            totalCartAdds = perArticle.sumOf { it.cartAdds },
            totalReservations = reservations.values.count { it.createdAt >= since },
            activeShoppers = recent.map { it.uid }.distinct().size,
            perArticle = perArticle,
            daily = daily,
        )
    }
}
