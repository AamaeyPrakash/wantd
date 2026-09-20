package com.tapshop.server.ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import com.tapshop.server.data.InMemoryStore
import com.tapshop.shared.model.CartItem
import com.tapshop.shared.model.formatPrice

/** Tools the Koog agent can call to ground its answers in the shopper's real data. */
@LLMDescription("Tools to read the shopper's wishlist, cart and the catalogue of tagged pieces, and to add to cart.")
class ShoppingTools(private val store: InMemoryStore, private val uid: String) : ToolSet {

    @Tool
    @LLMDescription("Returns the pieces the shopper saved to their wishlist, with id, brand, price, material and the store where they scanned it.")
    fun getWishlist(): String {
        val items = store.wishlist(uid)
        if (items.isEmpty()) return "The wishlist is empty."
        return items.joinToString("\n") { e ->
            "- id=${e.article.id} | ${e.article.name} by ${e.article.brand} | ${formatPrice(e.article.priceCents, e.article.currency)} | ${e.article.material} | " +
                "colours: ${e.article.colors.joinToString { it.name }} | scanned at ${e.store.brand} (${e.store.location})"
        }
    }

    @Tool
    @LLMDescription("Returns the shopper's current cart with quantities and the running total.")
    fun getCart(): String {
        val items = store.cart(uid)
        if (items.isEmpty()) return "The cart is empty."
        val total = items.sumOf { it.article.priceCents * it.item.quantity }
        return items.joinToString("\n") { e ->
            "- id=${e.article.id} | ${e.article.name} x${e.item.quantity} | ${formatPrice(e.article.priceCents, e.article.currency)} each" +
                (e.item.size?.let { " | size $it" } ?: "") + (e.item.color?.let { " | colour $it" } ?: "")
        } + "\nTotal: ${formatPrice(total, items.first().article.currency)}"
    }

    @Tool
    @LLMDescription("Returns full details for one piece by its id: description, material, care, sizes, colours, price, stock and store location.")
    fun getArticle(
        @LLMDescription("The article id, e.g. linen-overshirt") articleId: String,
    ): String {
        val a = store.article(articleId) ?: return "No piece with id $articleId."
        val s = store.store(a.storeId)
        return """
            ${a.name} by ${a.brand} (id=${a.id})
            Category: ${a.category} | Price: ${formatPrice(a.priceCents, a.currency)} | Stock: ${a.stock}
            Material: ${a.material} | Care: ${a.care}
            Colours: ${a.colors.joinToString { it.name }} | Sizes: ${a.sizes.joinToString()}
            Store: ${s?.let { "${it.brand} ${it.name}, ${it.location}, ${it.city}" } ?: a.storeId}
            Description: ${a.description}
            Tags: ${a.tags.joinToString()}
        """.trimIndent()
    }

    @Tool
    @LLMDescription("Lists every tagged piece available across all participating stores (id, name, brand, price, category).")
    fun listCatalogue(): String =
        store.articles().joinToString("\n") { a ->
            "- id=${a.id} | ${a.name} by ${a.brand} | ${a.category} | ${formatPrice(a.priceCents, a.currency)} | ${a.material}"
        }

    @Tool
    @LLMDescription("Adds a piece to the shopper's cart. Only call this when the shopper explicitly asks to add or buy something.")
    fun addToCart(
        @LLMDescription("The article id to add") articleId: String,
        @LLMDescription("Optional size label, e.g. M") size: String? = null,
        @LLMDescription("Optional colour name, e.g. Navy") color: String? = null,
    ): String {
        val article = store.article(articleId) ?: return "No piece with id $articleId."
        store.addToCart(uid, CartItem(articleId = articleId, size = size, color = color, quantity = 1))
        return "Added ${article.name} to the cart."
    }
}
