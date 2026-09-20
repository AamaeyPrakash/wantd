package com.tapshop.server.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAILLMClient
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.AttachmentContent
import ai.koog.prompt.message.AttachmentSource
import com.tapshop.server.Config
import com.tapshop.server.data.Images
import com.tapshop.server.data.InMemoryStore
import com.tapshop.shared.api.AppJson
import com.tapshop.shared.i18n.Language
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ChatMessage
import com.tapshop.shared.model.ChatResponse
import com.tapshop.shared.model.CompareCriterion
import com.tapshop.shared.model.CompareRequest
import com.tapshop.shared.model.CompareResult
import com.tapshop.shared.model.formatPrice
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * The AI personal shopping assistant, built on Koog (JetBrains' Kotlin agent framework).
 *
 * - [compare] sends the shopper's selected pieces (facts + product photos + optional photos of the
 *   shopper) to a vision-capable OpenAI model through Koog's prompt DSL and asks for a structured verdict.
 * - [chat] runs a Koog [AIAgent] with tools that read the shopper's real wishlist / cart / catalogue.
 * - When there is no API key (or AI_MOCK=true) both fall back to a deterministic demo answer so the
 *   presentation never depends on network or quota.
 */
class ShoppingAssistant(private val store: InMemoryStore) {
    private val log = LoggerFactory.getLogger(ShoppingAssistant::class.java)

    val mock: Boolean = Config.aiMock
    private val executor: PromptExecutor? = Config.openAiApiKey?.let { MultiLLMPromptExecutor(OpenAILLMClient(it)) }

    /** Vision-capable models to try, in order. The first one is overridable via OPENAI_MODEL. */
    private val models: List<LLModel> = buildList {
        Config.openAiModel?.let { modelByName(it) }?.let(::add)
        add(OpenAIModels.Chat.GPT5_4Mini)
        add(OpenAIModels.Chat.GPT5Mini)
        add(OpenAIModels.Chat.GPT4_1)
        add(OpenAIModels.Chat.GPT4o)
    }.distinctBy { it.id }

    private fun modelByName(name: String): LLModel? = when (name.lowercase().replace('_', '-')) {
        "gpt-4o" -> OpenAIModels.Chat.GPT4o
        "gpt-4o-mini" -> OpenAIModels.Chat.GPT4oMini
        "gpt-4.1" -> OpenAIModels.Chat.GPT4_1
        "gpt-4.1-mini" -> OpenAIModels.Chat.GPT4_1Mini
        "gpt-5" -> OpenAIModels.Chat.GPT5
        "gpt-5-mini" -> OpenAIModels.Chat.GPT5Mini
        "gpt-5.1" -> OpenAIModels.Chat.GPT5_1
        "gpt-5.2" -> OpenAIModels.Chat.GPT5_2
        "gpt-5.4" -> OpenAIModels.Chat.GPT5_4
        "gpt-5.4-mini" -> OpenAIModels.Chat.GPT5_4Mini
        "gpt-5.5" -> OpenAIModels.Chat.GPT5_5
        else -> null
    }

    // ---- Compare -------------------------------------------------------------------------------

    suspend fun compare(request: CompareRequest): CompareResult {
        val articles = request.articleIds.mapNotNull(store::article)
        if (articles.size < 2) {
            return CompareResult(summary = "Select at least two pieces to compare.", mock = true)
        }
        val language = Language.fromCode(request.language) ?: Language.EN
        if (mock || executor == null) return mockCompare(articles, request.question, language)

        val userPhotos = request.userImagesBase64.mapNotNull(::decodeDataUrl)
        val prompt = prompt("tapshop-compare") {
            system(compareSystemPrompt(language, articles.map { it.id }))
            user {
                text(compareUserText(articles, request.question, userPhotos.size))
                articles.forEachIndexed { index, article ->
                    val file = article.images.firstOrNull() ?: "${article.id}.jpg"
                    image(
                        AttachmentSource.Image(
                            content = AttachmentContent.Binary.Bytes(Images.bytes(file, article.name)),
                            format = file.substringAfterLast('.', "jpg"),
                            mimeType = Images.contentType(file),
                            fileName = "article-${index + 1}-${article.id}.${file.substringAfterLast('.', "jpg")}",
                        ),
                    )
                }
                userPhotos.forEachIndexed { index, (mime, bytes) ->
                    image(
                        AttachmentSource.Image(
                            content = AttachmentContent.Binary.Bytes(bytes),
                            format = mime.substringAfter('/'),
                            mimeType = mime,
                            fileName = "shopper-photo-${index + 1}.${mime.substringAfter('/')}",
                        ),
                    )
                }
            }
        }

        var lastError: Throwable? = null
        for (model in models) {
            try {
                val response = executor.execute(prompt, model)
                val text = response.textContent()
                log.info("Compare answered by ${model.id} (${text.length} chars)")
                return parseCompare(text, articles)
            } catch (t: Throwable) {
                lastError = t
                log.warn("Compare failed with model ${model.id}: ${t.message}")
            }
        }
        log.error("All models failed, returning mock comparison", lastError)
        return mockCompare(articles, request.question, language).copy(
            summary = "(AI unavailable: ${lastError?.message?.take(120)}) " + mockCompare(articles, request.question, language).summary,
        )
    }

    private fun compareSystemPrompt(language: Language, ids: List<String>): String = """
        You are TapShop's personal shopping assistant: a warm, honest stylist with a textile expert's eye.
        Shoppers save clothing pieces while walking through physical stores by tapping NFC tags or scanning QR codes,
        then ask you to compare them before buying. You see each piece's facts and its product photo, and sometimes
        photos of the shopper or their outfit.

        Respond ONLY with a single JSON object, no markdown fences, matching exactly:
        {
          "summary": "2-3 sentence overall verdict",
          "winnerArticleId": "one of ${ids.joinToString(" | ")}",
          "criteria": [
            { "name": "criterion name", "scores": { "<articleId>": <integer 1-10>, ... }, "note": "one crisp sentence comparing the pieces on this criterion" }
          ],
          "recommendation": "2-3 sentences: which to buy, for whom/what occasion, and one styling tip"
        }
        Use exactly these five criteria (translated): Fit & silhouette; Colour & versatility; Material quality;
        Price-to-quality; Occasion match. Every criterion must score EVERY article id. Be decisive and specific:
        reference materials, cuts, colours and prices you actually see. If shopper photos are provided, judge how each
        piece would suit their colouring, proportions and the rest of the outfit.
        Write all natural-language text in ${language.englishName}.
    """.trimIndent()

    private fun compareUserText(articles: List<Article>, question: String?, userPhotoCount: Int): String = buildString {
        appendLine("Pieces to compare (product photos are attached in the same order):")
        articles.forEachIndexed { i, a -> appendLine(articleFacts(i + 1, a)) }
        if (userPhotoCount > 0) {
            appendLine()
            appendLine("After the product photos, $userPhotoCount photo(s) of the shopper / their outfit are attached. Use them to judge fit and colour harmony.")
        }
        if (!question.isNullOrBlank()) {
            appendLine()
            appendLine("The shopper's question: \"$question\". Let it drive the 'Occasion match' criterion and the recommendation.")
        }
    }

    private fun articleFacts(index: Int, a: Article): String {
        val storeName = store.store(a.storeId)?.let { "${it.brand} · ${it.name}, ${it.location}" } ?: a.brand
        return """
            $index. id="${a.id}" — ${a.name} by ${a.brand}
               Category: ${a.category} · Price: ${formatPrice(a.priceCents, a.currency)} · Stock: ${a.stock}
               Material: ${a.material}
               Colours: ${a.colors.joinToString { it.name }} · Sizes: ${a.sizes.joinToString()}
               Store: $storeName
               Description: ${a.description}
        """.trimIndent()
    }

    private fun parseCompare(raw: String, articles: List<Article>): CompareResult {
        val cleaned = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        val json = if (start >= 0 && end > start) cleaned.substring(start, end + 1) else cleaned
        return try {
            val parsed = AppJson.decodeFromString(CompareResult.serializer(), json)
            val validWinner = parsed.winnerArticleId?.takeIf { id -> articles.any { it.id == id } }
                ?: parsed.criteria.flatMap { it.scores.entries }.groupBy({ it.key }, { it.value })
                    .maxByOrNull { (_, v) -> v.sum() }?.key
            parsed.copy(winnerArticleId = validWinner, mock = false)
        } catch (t: Throwable) {
            log.warn("Could not parse structured comparison, returning free text: ${t.message}")
            CompareResult(summary = raw.trim(), mock = false)
        }
    }

    // ---- Chat (agent with tools) ----------------------------------------------------------------

    suspend fun chat(uid: String, messages: List<ChatMessage>, languageCode: String): ChatResponse {
        val language = Language.fromCode(languageCode) ?: Language.EN
        val latest = messages.lastOrNull { it.role == "user" }?.content?.trim().orEmpty()
        if (mock || executor == null) return ChatResponse(reply = mockChat(uid, latest, language), mock = true)

        val history = messages.dropLast(1).takeLast(8).joinToString("\n") { "${it.role.uppercase()}: ${it.content}" }
        val input = buildString {
            if (history.isNotBlank()) {
                appendLine("Conversation so far:")
                appendLine(history)
                appendLine()
            }
            append("USER: $latest")
        }

        val tools = ToolRegistry.builder().tools(ShoppingTools(store, uid)).build()
        var lastError: Throwable? = null
        for (model in models) {
            try {
                val agent = AIAgent(
                    promptExecutor = executor,
                    llmModel = model,
                    toolRegistry = tools,
                    systemPrompt = chatSystemPrompt(language),
                    maxIterations = 12,
                )
                val reply = agent.run(input)
                return ChatResponse(reply = reply.trim(), mock = false)
            } catch (t: Throwable) {
                lastError = t
                log.warn("Chat failed with model ${model.id}: ${t.message}")
            }
        }
        log.error("All models failed for chat, returning mock reply", lastError)
        return ChatResponse(reply = mockChat(uid, latest, language), mock = true)
    }

    private fun chatSystemPrompt(language: Language) = """
        You are TapShop's personal shopping assistant inside a shopper's phone. The shopper walks through physical
        stores, taps NFC tags / scans QR codes on clothes and saves them to a wishlist. You have tools to read their
        wishlist, their cart, the full catalogue of tagged pieces, and to add a piece to their cart when they ask.
        Always look at the real wishlist before advising. Be concise (max ~120 words), friendly and concrete:
        name pieces, prices, materials and where they are in the mall. Never invent pieces that are not in the tools.
        Reply in ${language.englishName}. Plain text only, no markdown headings.
    """.trimIndent()

    // ---- Mock fallbacks ----------------------------------------------------------------------------

    private fun mockCompare(articles: List<Article>, question: String?, language: Language): CompareResult {
        fun score(a: Article, criterion: String): Int {
            val m = a.material.lowercase()
            return when (criterion) {
                "fit" -> 6 + (a.sizes.size.coerceAtMost(4)) / 2 + if (a.category == "Trousers") 1 else 0
                "colour" -> 5 + a.colors.size.coerceAtMost(4)
                "material" -> when {
                    "merino" in m || "silk" in m -> 9
                    "wool" in m || "linen" in m -> 8
                    else -> 6
                }
                "price" -> (10 - (a.priceCents / 3000)).toInt().coerceIn(4, 9)
                else -> 6 + (a.tags.size.coerceAtMost(3))
            }.coerceIn(1, 10)
        }

        val names = when (language) {
            Language.FR -> listOf("Coupe et silhouette", "Couleur et polyvalence", "Qualité de la matière", "Rapport qualité-prix", "Adéquation à l'occasion")
            Language.ES -> listOf("Ajuste y silueta", "Color y versatilidad", "Calidad del material", "Relación calidad-precio", "Adecuación a la ocasión")
            Language.JA -> listOf("フィット感とシルエット", "色と着回し", "素材の品質", "価格と品質のバランス", "シーンへの適合")
            Language.AR -> listOf("القصّة والشكل", "اللون والتنوع", "جودة الخامة", "القيمة مقابل السعر", "مناسبة الموقف")
            Language.EN -> listOf("Fit & silhouette", "Colour & versatility", "Material quality", "Price-to-quality", "Occasion match")
        }
        val keys = listOf("fit", "colour", "material", "price", "occasion")
        val criteria = keys.mapIndexed { i, key ->
            CompareCriterion(
                name = names[i],
                scores = articles.associate { it.id to score(it, key) },
                note = mockNote(key, articles, language),
            )
        }
        val totals = articles.associateWith { a -> criteria.sumOf { it.scores[a.id] ?: 0 } }
        val winner = totals.maxByOrNull { it.value }!!.key
        val runnerUp = articles.filter { it != winner }.maxByOrNull { totals[it] ?: 0 }

        val summary = when (language) {
            Language.FR -> "${winner.name} de ${winner.brand} l'emporte : ${winner.material.lowercase()} pour ${formatPrice(winner.priceCents, winner.currency)}, la pièce la plus équilibrée entre qualité, polyvalence et prix." +
                (runnerUp?.let { " ${it.name} reste une belle alternative si vous cherchez ${it.tags.firstOrNull() ?: "autre chose"}." } ?: "")
            Language.ES -> "${winner.name} de ${winner.brand} gana: ${winner.material.lowercase()} por ${formatPrice(winner.priceCents, winner.currency)}, la prenda más equilibrada entre calidad, versatilidad y precio." +
                (runnerUp?.let { " ${it.name} es una buena alternativa si buscas algo más ${it.tags.firstOrNull() ?: "distinto"}." } ?: "")
            Language.JA -> "${winner.brand}の${winner.name}がおすすめです。${winner.material}で${formatPrice(winner.priceCents, winner.currency)}、品質・着回し・価格のバランスが最も良い一着です。" +
                (runnerUp?.let { "${it.name}も${it.tags.firstOrNull() ?: "別のシーン"}向けには良い選択肢です。" } ?: "")
            Language.AR -> "${winner.name} من ${winner.brand} هو الاختيار الأفضل: ${winner.material} بسعر ${formatPrice(winner.priceCents, winner.currency)}، الأكثر توازنًا بين الجودة والتنوع والسعر." +
                (runnerUp?.let { " ${it.name} بديل جيد إذا كنت تبحث عن ${it.tags.firstOrNull() ?: "شيء مختلف"}." } ?: "")
            Language.EN -> "${winner.name} by ${winner.brand} takes it: ${winner.material.lowercase()} at ${formatPrice(winner.priceCents, winner.currency)} is the best balance of quality, versatility and price in this set." +
                (runnerUp?.let { " ${it.name} is a strong alternative if you want something more ${it.tags.firstOrNull() ?: "different"}." } ?: "")
        }
        val recommendation = when (language) {
            Language.FR -> "Achetez ${winner.name} en ${winner.colors.firstOrNull()?.name ?: "la teinte principale"} : il se portera avec tout ce que vous avez déjà. ${question?.let { "Pour « $it », c'est aussi le choix le plus sûr." } ?: "Portez-le avec une pièce neutre pour le laisser parler."}"
            Language.ES -> "Compra ${winner.name} en ${winner.colors.firstOrNull()?.name ?: "el tono principal"}: combinará con todo lo que ya tienes. ${question?.let { "Para \"$it\" también es la opción más segura." } ?: "Llévalo con una prenda neutra para que destaque."}"
            Language.JA -> "${winner.name}の${winner.colors.firstOrNull()?.name ?: "メインカラー"}を選びましょう。手持ちの服と幅広く合わせられます。${question?.let { "「$it」にも最も安心な選択です。" } ?: "ニュートラルなアイテムと合わせると主役になります。"}"
            Language.AR -> "اشترِ ${winner.name} بلون ${winner.colors.firstOrNull()?.name ?: "اللون الأساسي"}: سيتناسب مع كل ما لديك. ${question?.let { "ولسؤالك \"$it\" فهو الخيار الأكثر أمانًا أيضًا." } ?: "نسّقه مع قطعة محايدة ليبرز."}"
            Language.EN -> "Buy ${winner.name} in ${winner.colors.firstOrNull()?.name ?: "the main colour"}: it will work with most of what you already own. ${question?.let { "For \"$it\" it is also the safest choice." } ?: "Pair it with something neutral and let it do the talking."}"
        }
        return CompareResult(summary = summary, winnerArticleId = winner.id, criteria = criteria, recommendation = recommendation, mock = true)
    }

    private fun mockNote(key: String, articles: List<Article>, language: Language): String {
        val a = articles.first()
        val b = articles.last()
        return when (language) {
            Language.EN -> when (key) {
                "fit" -> "${a.name} has the more relaxed cut; ${b.name} sits closer to the body."
                "colour" -> "${articles.maxByOrNull { it.colors.size }?.name} offers the widest palette to build outfits around."
                "material" -> "${articles.maxByOrNull { it.material.length }?.name}'s ${articles.maxByOrNull { it.material.length }?.material?.lowercase()} feels the most premium in hand."
                "price" -> "${articles.minByOrNull { it.priceCents }?.name} is the cheapest at ${articles.minByOrNull { it.priceCents }?.let { formatPrice(it.priceCents, it.currency) }} without a big drop in quality."
                else -> "${a.name} skews casual; ${b.name} dresses up more easily."
            }
            Language.FR -> when (key) {
                "fit" -> "${a.name} a la coupe la plus décontractée ; ${b.name} est plus près du corps."
                "colour" -> "${articles.maxByOrNull { it.colors.size }?.name} offre la palette la plus large."
                "material" -> "${articles.maxByOrNull { it.material.length }?.name} a le toucher le plus premium."
                "price" -> "${articles.minByOrNull { it.priceCents }?.name} est la moins chère sans grande perte de qualité."
                else -> "${a.name} est plus casual ; ${b.name} s'habille plus facilement."
            }
            Language.ES -> when (key) {
                "fit" -> "${a.name} tiene el corte más relajado; ${b.name} se ajusta más al cuerpo."
                "colour" -> "${articles.maxByOrNull { it.colors.size }?.name} ofrece la paleta más amplia."
                "material" -> "${articles.maxByOrNull { it.material.length }?.name} tiene el tacto más premium."
                "price" -> "${articles.minByOrNull { it.priceCents }?.name} es la más barata sin perder calidad."
                else -> "${a.name} es más informal; ${b.name} se viste con más facilidad."
            }
            Language.JA -> when (key) {
                "fit" -> "${a.name}はよりリラックスした作り、${b.name}は体に沿うシルエットです。"
                "colour" -> "${articles.maxByOrNull { it.colors.size }?.name}が最も色展開が豊富です。"
                "material" -> "${articles.maxByOrNull { it.material.length }?.name}が最も上質な手触りです。"
                "price" -> "${articles.minByOrNull { it.priceCents }?.name}が品質を保ちつつ最も手頃です。"
                else -> "${a.name}はカジュアル寄り、${b.name}はきれいめにも使えます。"
            }
            Language.AR -> when (key) {
                "fit" -> "${a.name} قصّته أكثر ارتياحًا؛ ${b.name} أقرب إلى الجسم."
                "colour" -> "${articles.maxByOrNull { it.colors.size }?.name} يقدّم أوسع تشكيلة ألوان."
                "material" -> "${articles.maxByOrNull { it.material.length }?.name} يمنح الملمس الأكثر فخامة."
                "price" -> "${articles.minByOrNull { it.priceCents }?.name} هو الأقل سعرًا دون تنازل كبير في الجودة."
                else -> "${a.name} يميل إلى الكاجوال؛ ${b.name} أسهل في التنسيق الرسمي."
            }
        }
    }

    private fun mockChat(uid: String, question: String, language: Language): String {
        val wishlist = store.wishlist(uid)
        if (wishlist.isEmpty()) {
            return when (language) {
                Language.FR -> "Votre liste est encore vide. Touchez une étiquette en boutique et je pourrai comparer vos pièces, proposer des tenues et repérer le meilleur rapport qualité-prix."
                Language.ES -> "Tu lista aún está vacía. Toca una etiqueta en la tienda y podré comparar tus prendas, sugerir conjuntos y encontrar la mejor compra."
                Language.JA -> "まだお気に入りが空です。店内でタグをタップすると、比較やコーディネート提案、コスパの良いアイテム探しをお手伝いできます。"
                Language.AR -> "قائمتك فارغة حتى الآن. المس بطاقة في المتجر وسأقارن قطعك وأقترح إطلالات وأجد الأفضل قيمة."
                Language.EN -> "Your wishlist is empty so far. Tap a tag in store and I can compare your pieces, build outfits and spot the best value."
            }
        }
        val best = wishlist.minByOrNull { it.article.priceCents.toDouble() / (it.article.material.length + 10) }!!.article
        val list = wishlist.joinToString(", ") { "${it.article.name} (${formatPrice(it.article.priceCents, it.article.currency)}, ${it.store.brand})" }
        return when (language) {
            Language.FR -> "Vous avez enregistré : $list. Le meilleur rapport qualité-prix est ${best.name} en ${best.material.lowercase()} pour ${formatPrice(best.priceCents, best.currency)}. ${if (question.isNotBlank()) "Concernant « $question » : " else ""}associez-le à une pièce neutre de votre liste et vous avez une tenue complète."
            Language.ES -> "Has guardado: $list. La mejor compra es ${best.name} en ${best.material.lowercase()} por ${formatPrice(best.priceCents, best.currency)}. ${if (question.isNotBlank()) "Sobre \"$question\": " else ""}combínala con una prenda neutra de tu lista y tienes un conjunto completo."
            Language.JA -> "保存済み：$list。最もコスパが良いのは${best.material}の${best.name}（${formatPrice(best.priceCents, best.currency)}）です。${if (question.isNotBlank()) "「$question」については、" else ""}リスト内のニュートラルなアイテムと合わせれば完成です。"
            Language.AR -> "لقد حفظت: $list. الأفضل قيمةً هو ${best.name} من ${best.material} بسعر ${formatPrice(best.priceCents, best.currency)}. ${if (question.isNotBlank()) "بخصوص \"$question\": " else ""}نسّقه مع قطعة محايدة من قائمتك لتحصل على إطلالة كاملة."
            Language.EN -> "You've saved: $list. Best value is ${best.name} in ${best.material.lowercase()} at ${formatPrice(best.priceCents, best.currency)}. ${if (question.isNotBlank()) "On \"$question\": " else ""}pair it with a neutral piece from your list and you have a complete outfit."
        }
    }

    private fun decodeDataUrl(value: String): Pair<String, ByteArray>? = try {
        if (value.startsWith("data:")) {
            val mime = value.substringAfter("data:").substringBefore(';').ifBlank { "image/jpeg" }
            val data = value.substringAfter("base64,")
            mime to Base64.getDecoder().decode(data)
        } else {
            "image/jpeg" to Base64.getDecoder().decode(value)
        }
    } catch (_: IllegalArgumentException) {
        null
    }
}
