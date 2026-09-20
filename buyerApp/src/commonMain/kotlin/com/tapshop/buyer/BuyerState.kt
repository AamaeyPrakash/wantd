package com.tapshop.buyer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tapshop.shared.api.ApiClient
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.CartEntry
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.ChatMessage
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.ServerInfo
import com.tapshop.shared.model.WishlistEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** All buyer-side state, backed by the server. One instance lives for the whole app session. */
class BuyerState(private val api: ApiClient, private val scope: CoroutineScope) {
    var articles by mutableStateOf<List<Article>>(emptyList())
        private set
    var wishlist by mutableStateOf<List<WishlistEntry>>(emptyList())
        private set
    var cart by mutableStateOf<List<CartEntry>>(emptyList())
        private set
    var serverInfo by mutableStateOf<ServerInfo?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    /** Chat transcript is kept here so it survives tab switches. */
    val chat = mutableStateListOf<ChatMessage>()

    val cartCount: Int get() = cart.sumOf { it.item.quantity }
    val cartTotalCents: Long get() = cart.sumOf { it.article.priceCents * it.item.quantity }

    fun isSaved(articleId: String) = wishlist.any { it.article.id == articleId }
    fun inCart(articleId: String) = cart.any { it.article.id == articleId }
    fun article(id: String): Article? = articles.firstOrNull { it.id == id }

    fun refresh() {
        scope.launch {
            loading = articles.isEmpty()
            error = null
            try {
                articles = api.articles()
                wishlist = api.wishlist()
                cart = api.cart()
                if (serverInfo == null) serverInfo = runCatching { api.info() }.getOrNull()
            } catch (t: Throwable) {
                error = t.message ?: "offline"
            } finally {
                loading = false
            }
        }
    }

    suspend fun loadArticle(id: String): Article? = article(id) ?: try {
        api.article(id).also { fetched -> if (articles.none { it.id == fetched.id }) articles = articles + fetched }
    } catch (_: Throwable) {
        null
    }

    fun track(articleId: String, type: EventType) {
        scope.launch { api.track(articleId, type) }
    }

    suspend fun toggleWishlist(articleId: String): Boolean {
        val saved = isSaved(articleId)
        wishlist = if (saved) api.removeFromWishlist(articleId) else api.addToWishlist(articleId)
        return !saved
    }

    suspend fun removeFromWishlist(articleId: String) {
        wishlist = api.removeFromWishlist(articleId)
    }

    suspend fun addToCart(item: CartItem) {
        cart = api.addToCart(item)
    }

    suspend fun updateCartQuantity(articleId: String, quantity: Int) {
        cart = api.updateCartQuantity(articleId, quantity)
    }

    suspend fun removeFromCart(articleId: String) {
        cart = api.removeFromCart(articleId)
    }

    suspend fun reserve(): Reservation {
        val reservation = api.reserve()
        cart = emptyList()
        return reservation
    }
}
