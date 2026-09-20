package com.tapshop.shared.api

object ApiRoutes {
    const val API = "/api"
    const val INFO = "$API/info"
    const val ARTICLES = "$API/articles"
    const val STORES = "$API/stores"
    const val USERS = "$API/users"
    const val EVENTS = "$API/events"
    const val ANALYTICS_SUMMARY = "$API/analytics/summary"
    const val RESERVATIONS = "$API/reservations"
    const val AI_COMPARE = "$API/ai/compare"
    const val AI_CHAT = "$API/ai/chat"
    const val IMAGES = "$API/images"
    const val QR_SHEET = "/qr-sheet"

    fun article(id: String) = "$ARTICLES/$id"
    fun articleQr(id: String) = "$ARTICLES/$id/qr.png"
    fun wishlist(uid: String) = "$USERS/$uid/wishlist"
    fun wishlistItem(uid: String, articleId: String) = "$USERS/$uid/wishlist/$articleId"
    fun cart(uid: String) = "$USERS/$uid/cart"
    fun cartItem(uid: String, articleId: String) = "$USERS/$uid/cart/$articleId"
    fun reserve(uid: String) = "$USERS/$uid/reserve"
    fun reservation(id: String) = "$RESERVATIONS/$id"
    fun image(file: String) = "$IMAGES/$file"

    /** The URL encoded into a QR code / NFC tag. Opens the buyer web app on the article. */
    fun tagUrl(publicBaseUrl: String, articleId: String) = "${publicBaseUrl.trimEnd('/')}/?a=$articleId"
}
