package com.tapshop.server.data

import com.tapshop.shared.model.AnalyticsEvent
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ColorOption
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.Store
import kotlin.random.Random

object Seed {
    val stores = listOf(
        Store(id = "maison-noor", brand = "Maison Noor", name = "Flagship", mall = "Dubai Mall", floor = "Level 1", city = "Dubai"),
        Store(id = "studio-kell", brand = "Studio Kell", name = "Atelier", mall = "Mall of the Emirates", floor = "Ground floor", city = "Dubai"),
        Store(id = "pitti-block", brand = "PITTI Block", name = "Concept store", mall = "City Walk", floor = "Unit 12", city = "Dubai"),
    )

    val articles = listOf(
        Article(
            id = "linen-overshirt",
            storeId = "maison-noor",
            brand = "Maison Noor",
            name = "Linen Overshirt",
            category = "Shirts",
            priceCents = 14900,
            description = "A relaxed overshirt cut from airy European linen. Wear it open over a tee in the heat or buttoned as a light layer in the evening. Garment-washed for a soft, lived-in hand feel from day one.",
            material = "100% European linen",
            care = "Machine wash cold, hang dry, warm iron",
            colors = listOf(ColorOption("Sand", "#D9C3A5"), ColorOption("Olive", "#6B7150"), ColorOption("White", "#F4F1EA")),
            sizes = listOf("XS", "S", "M", "L", "XL"),
            images = listOf("linen-overshirt.jpg"),
            stock = 14,
            tags = listOf("summer", "breathable", "layering", "casual"),
        ),
        Article(
            id = "merino-crewneck",
            storeId = "studio-kell",
            brand = "Studio Kell",
            name = "Merino Crewneck",
            category = "Knitwear",
            priceCents = 21500,
            description = "A fine-gauge crewneck in extra-fine merino that regulates temperature and resists odour. Clean ribbed trims and a slightly cropped body make it work with tailoring or denim.",
            material = "100% extra-fine merino wool (18.5 micron)",
            care = "Hand wash or wool cycle, dry flat",
            colors = listOf(ColorOption("Navy", "#1F2A44"), ColorOption("Oat", "#D8CFC0"), ColorOption("Charcoal", "#3A3A3C")),
            sizes = listOf("S", "M", "L", "XL"),
            images = listOf("merino-crewneck.jpg"),
            stock = 6,
            tags = listOf("knitwear", "smart", "layering", "all-season"),
        ),
        Article(
            id = "silk-bamboo-jacket",
            storeId = "pitti-block",
            brand = "PITTI Block",
            name = "Silk-Bamboo Graphic Jacket",
            category = "Jackets",
            priceCents = 11299,
            description = "A boxy sweat-jacket in a silk and bamboo blend with a playful embroidered graphic on the chest. Drapes like silk, breathes like bamboo, and packs a lot of personality for weekends.",
            material = "55% silk, 45% bamboo viscose",
            care = "Dry clean or gentle hand wash",
            colors = listOf(ColorOption("Red", "#C8102E"), ColorOption("Grey", "#B5B5B8"), ColorOption("Blue", "#2F5DA8")),
            sizes = listOf("S", "M", "L"),
            images = listOf("silk-bamboo-jacket.jpg"),
            stock = 9,
            tags = listOf("statement", "weekend", "soft", "graphic"),
        ),
        Article(
            id = "tailored-wool-trousers",
            storeId = "studio-kell",
            brand = "Studio Kell",
            name = "Tailored Wool Trousers",
            category = "Trousers",
            priceCents = 19500,
            description = "Straight-leg trousers in a tropical-weight wool with a touch of stretch. Pressed front crease, side adjusters instead of belt loops, and a cropped break that sits neatly on loafers or trainers.",
            material = "98% wool, 2% elastane",
            care = "Dry clean only",
            colors = listOf(ColorOption("Charcoal", "#3A3A3C"), ColorOption("Camel", "#B98C5A")),
            sizes = listOf("46", "48", "50", "52", "54"),
            images = listOf("tailored-wool-trousers.jpg"),
            stock = 11,
            tags = listOf("tailoring", "office", "wedding", "smart"),
        ),
    )

    fun apply(store: InMemoryStore) {
        stores.forEach(store::putStore)
        articles.forEach(store::upsertArticle)
        seedEvents(store)
    }

    /** Seven days of plausible engagement so the merchant charts are alive on first launch. */
    private fun seedEvents(store: InMemoryStore) {
        val random = Random(42)
        val now = System.currentTimeMillis()
        val dayMs = 24L * 3600_000L
        // Relative popularity per article, so the charts have a story.
        val weights = mapOf(
            "linen-overshirt" to 1.4,
            "merino-crewneck" to 1.0,
            "silk-bamboo-jacket" to 1.7,
            "tailored-wool-trousers" to 0.8,
        )
        for (back in 6 downTo 0) {
            val dayStart = now - back * dayMs
            for (article in articles) {
                val w = weights[article.id] ?: 1.0
                val scans = (random.nextInt(9, 22) * w).toInt()
                repeat(scans) { i ->
                    val uid = "demo-shopper-${random.nextInt(1, 60)}"
                    val t = dayStart - random.nextLong(0, if (back == 0) 6 * 3600_000L else dayMs - 1)
                    store.record(AnalyticsEvent(uid, article.id, EventType.SCAN, t))
                    store.record(AnalyticsEvent(uid, article.id, EventType.VIEW, t + 800))
                    if (random.nextDouble() < 0.42) store.record(AnalyticsEvent(uid, article.id, EventType.WISHLIST_ADD, t + 9_000))
                    if (random.nextDouble() < 0.16) store.record(AnalyticsEvent(uid, article.id, EventType.CART_ADD, t + 25_000))
                    if (random.nextDouble() < 0.06) store.record(AnalyticsEvent(uid, article.id, EventType.SHARE, t + 30_000))
                    if (random.nextDouble() < 0.11) store.record(AnalyticsEvent(uid, article.id, EventType.COMPARE, t + 60_000))
                }
            }
        }
    }
}
