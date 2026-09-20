package com.tapshop.server.ai

import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.prompt.Prompt
import ai.koog.prompt.dsl.ModerationResult
import ai.koog.prompt.llm.LLModel
import ai.koog.prompt.message.Message
import ai.koog.prompt.message.ResponseMetaInfo
import ai.koog.prompt.streaming.StreamFrame
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.tapshop.server.Config
import com.tapshop.server.data.InMemoryStore
import com.tapshop.server.module
import com.tapshop.server.routes.apiRoutes
import com.tapshop.shared.api.AppJson
import com.tapshop.shared.model.*
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsBytes
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.Flow
import kotlin.time.Clock
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.*

class ShoppingAssistantTest {
    private val store = InMemoryStore().apply {
        putStore(Store("shop", "Brand", "Store", "Mall", "1", "City"))
    }
    private val assistant = ShoppingAssistant(store, executor = null)
    private val any = ComparePreferences(ColorPreference.ANY, FitPreference.ANY, OccasionPreference.ANY, ShoppingPriority.ANY)

    private fun article(id: String, price: Long = 5000, category: String = "Shirt") =
        Article(id, "shop", "Brand", id, category, price, description = "Relaxed fit",
            material = "Cotton", colors = listOf(ColorOption("Navy", "#222222")), stock = 5)

    @Test
    fun `model receives all answers and current product facts`() {
        val preferences = ComparePreferences(ColorPreference.LIGHT, FitPreference.TAILORED, OccasionPreference.WORK, ShoppingPriority.QUALITY)
        val text = assistant.compareUserText(listOf(article("first"), article("second")), preferences)
        listOf("LIGHT", "TAILORED", "WORK", "QUALITY", "first", "second", "$50.00", "Cotton", "Navy").forEach {
            assertTrue(it in text, "Missing $it")
        }
    }

    @Test
    fun `API suggestion and reasoning are preserved without a canned headline`() {
        val articles = listOf(article("first"), article("second"))
        val summary = "You should get second for your work wardrobe."
        val reason = "Its relaxed cut matches your preference at $50.00."
        val raw = AppJson.encodeToString(CompareResult.serializer(), CompareResult(summary, "second", reason))
        val result = assistant.parseCompare(raw, articles)
        assertEquals(summary, result.summary)
        assertEquals(reason, result.recommendation)
        assertEquals("second", result.winnerArticleId)
        assertFalse(result.mock)
    }

    @Test
    fun `comparison calls the executor and returns its answer`() = runBlocking {
        store.upsertArticle(article("first"))
        store.upsertArticle(article("second"))
        val expected = CompareResult("You should get second for work.", "second", "Its relaxed cotton cut matches your preference.")
        val executor = TestExecutor(AppJson.encodeToString(CompareResult.serializer(), expected))
        val result = ShoppingAssistant(store, executor).compare(CompareRequest("shopper", listOf("first", "second"), any))
        assertEquals(expected, result)
        assertEquals(1, executor.calls)
    }

    @Test
    fun `invalid or excessive model responses are rejected`() {
        val articles = listOf(article("first"), article("second"))
        val badResponses = listOf(
            "Unstructured response",
            """{"summary":"Get invented","winnerArticleId":"invented","recommendation":"A good match."}""",
            """{"summary":"Get first","winnerArticleId":"first","recommendation":""}""",
            """{"summary":"Get first","winnerArticleId":"first","recommendation":"${"long ".repeat(90)}"}""",
            """{"summary":"Get something else","winnerArticleId":"first","recommendation":"A good match."}""",
        )
        badResponses.forEach { raw -> assertFails { assistant.parseCompare(raw, articles) } }
    }

    @Test
    fun `missing credentials fail instead of returning recommendations`() = runBlocking {
        store.upsertArticle(article("first"))
        store.upsertArticle(article("second"))
        assertFalse(assistant.enabled)
        assertFailsWith<AiUnavailableException> {
            assistant.compare(CompareRequest("shopper", listOf("first", "second"), any))
        }
        assertFailsWith<AiUnavailableException> {
            assistant.chat("shopper", listOf(ChatMessage("user", "What goes with my cart?")), "en")
        }
        Unit
    }

    @Test
    fun `failed executor never falls back to prewritten answers`() = runBlocking {
        // Deliberate provider failure; this executor never uses the network.
        val failing = ShoppingAssistant(store, TestExecutor())
        store.upsertArticle(article("first"))
        store.upsertArticle(article("second"))
        assertFailsWith<AiUnavailableException> {
            failing.compare(CompareRequest("shopper", listOf("first", "second"), any))
        }
        assertFailsWith<AiUnavailableException> {
            failing.chat("shopper", listOf(ChatMessage("user", "Recommend something")), "en")
        }
        Unit
    }

    @Test
    fun `tools expose cart choices and unsaved catalogue items for the right shopper`() {
        val shirt = store.upsertArticle(article("cart-shirt", price = 1000))
        val trousers = store.upsertArticle(article("catalogue-trousers", category = "Trousers"))
        store.addToCart("shopper", CartItem(shirt.id, "M", "Navy", 2))
        val tools = ShoppingTools(store, "shopper")
        assertEquals("The wishlist is empty.", tools.getWishlist())
        val cart = tools.getCart()
        listOf("cart-shirt", "size M", "colour Navy", "Shirt", "Cotton", "$20.00").forEach { assertTrue(it in cart) }
        assertTrue(trousers.id in tools.listCatalogue())
        assertTrue("Relaxed fit" in tools.getArticle(trousers.id))
        assertEquals("The cart is empty.", ShoppingTools(store, "another-shopper").getCart())
        store.addToWishlist("shopper", trousers.id)
        assertTrue(trousers.id in tools.getWishlist())
    }

    @Test
    fun `missing key returns HTTP 503 for both AI endpoints`() = testApplication {
        application { module() }
        val compare = client.post("/api/ai/compare") {
            contentType(ContentType.Application.Json)
            setBody(AppJson.encodeToString(CompareRequest.serializer(), CompareRequest("shopper", listOf("linen-overshirt", "merino-crewneck"), any)))
        }
        val chat = client.post("/api/ai/chat") {
            contentType(ContentType.Application.Json)
            setBody(AppJson.encodeToString(ChatRequest.serializer(), ChatRequest("shopper", listOf(ChatMessage("user", "Recommend something")))))
        }
        listOf(compare, chat).forEach {
            assertEquals(HttpStatusCode.ServiceUnavailable, it.status)
            assertTrue("OPENAI_API_KEY" in it.bodyAsText())
            assertFalse("You should get" in it.bodyAsText())
        }
        val info = AppJson.decodeFromString(ServerInfo.serializer(), client.get("/api/info").bodyAsText())
        assertFalse(info.aiEnabled)
        assertFalse(info.aiMock)
    }

    @Test
    fun `compare API rejects invalid pairs and requires all four answers`() = testApplication {
        listOf("first", "second", "third").forEach { store.upsertArticle(article(it)) }
        application {
            install(ContentNegotiation) { json(AppJson) }
            routing { apiRoutes(store, assistant) }
        }
        suspend fun compare(ids: List<String>) = client.post("/api/ai/compare") {
            contentType(ContentType.Application.Json)
            setBody(AppJson.encodeToString(CompareRequest.serializer(), CompareRequest("shopper", ids, any)))
        }
        for (ids in listOf(emptyList(), listOf("first"), listOf("first", "first"), listOf("first", "second", "third"))) {
            assertEquals(HttpStatusCode.BadRequest, compare(ids).status)
        }
        assertEquals(HttpStatusCode.NotFound, compare(listOf("first", "missing")).status)
        for (body in listOf(
            """{"uid":"shopper","articleIds":["first","second"]}""",
            """{"uid":"shopper","articleIds":["first","second"],"preferences":{"color":"ANY"}}""",
            """{"uid":"shopper","articleIds":["first","second"],"preferences":{"color":"invalid","fit":"ANY","occasion":"ANY","priority":"ANY"}}""",
        )) {
            val response = client.post("/api/ai/compare") { contentType(ContentType.Application.Json); setBody(body) }
            assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        }
        assertTrue(store.summary(7).perArticle.all { it.compares == 0 })
    }

    @Test
    fun `product QR round trip uses public HTTPS base URL and article deep link`() = testApplication {
        application { module() }
        val response = client.get("/api/articles/linen-overshirt/qr.png?size=600")
        assertEquals(HttpStatusCode.OK, response.status)
        val image = ImageIO.read(ByteArrayInputStream(response.bodyAsBytes()))
        val decoded = MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(BufferedImageLuminanceSource(image))))
        assertTrue(Config.publicBaseUrl.startsWith("https://"))
        assertEquals("${Config.publicBaseUrl}/?a=linen-overshirt", decoded.text)
        assertTrue(decoded.text in client.get("/qr-sheet").bodyAsText())
    }

    /** API fixtures exist only in tests; the application has no mock-response path. */
    private class TestExecutor(private val reply: String? = null) : PromptExecutor() {
        var calls = 0
        override suspend fun execute(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Message.Assistant {
            calls++
            return Message.Assistant(reply ?: error("API fixture failure"), ResponseMetaInfo(Clock.System.now()))
        }
        override fun executeStreaming(prompt: Prompt, model: LLModel, tools: List<ToolDescriptor>): Flow<StreamFrame> =
            error("Streaming is not used by these tests")
        override suspend fun moderate(prompt: Prompt, model: LLModel): ModerationResult =
            error("Moderation is not used by these tests")
        override fun close() = Unit
    }
}
