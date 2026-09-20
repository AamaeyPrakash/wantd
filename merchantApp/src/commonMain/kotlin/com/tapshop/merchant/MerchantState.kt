package com.tapshop.merchant

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tapshop.shared.api.ApiClient
import com.tapshop.shared.model.AnalyticsSummary
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.Reservation
import com.tapshop.shared.model.ReservationStatus
import com.tapshop.shared.model.ServerInfo
import com.tapshop.shared.model.Store
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Merchant dashboard state. Polls the server so scans from phones show up live during the demo. */
class MerchantState(private val api: ApiClient, private val scope: CoroutineScope) {
    var articles by mutableStateOf<List<Article>>(emptyList())
        private set
    var stores by mutableStateOf<List<Store>>(emptyList())
        private set
    var summary by mutableStateOf<AnalyticsSummary?>(null)
        private set
    var reservations by mutableStateOf<List<Reservation>>(emptyList())
        private set
    var serverInfo by mutableStateOf<ServerInfo?>(null)
        private set
    var loading by mutableStateOf(true)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var lastUpdated by mutableStateOf(0L)
        private set

    private var pollJob: Job? = null

    fun store(id: String): Store? = stores.firstOrNull { it.id == id }

    fun startPolling(intervalMillis: Long = 4000) {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                refreshNow()
                delay(intervalMillis)
            }
        }
    }

    fun refresh() {
        scope.launch { refreshNow() }
    }

    private suspend fun refreshNow() {
        try {
            if (stores.isEmpty()) stores = api.stores()
            if (serverInfo == null) serverInfo = runCatching { api.info() }.getOrNull()
            articles = api.articles()
            summary = api.analyticsSummary()
            reservations = api.reservations()
            error = null
            lastUpdated++
        } catch (t: Throwable) {
            error = t.message ?: "offline"
        } finally {
            loading = false
        }
    }

    suspend fun saveArticle(article: Article, isNew: Boolean): Article {
        val saved = if (isNew) api.createArticle(article) else api.updateArticle(article)
        articles = if (isNew) articles + saved else articles.map { if (it.id == saved.id) saved else it }
        return saved
    }

    suspend fun deleteArticle(id: String) {
        api.deleteArticle(id)
        articles = articles.filterNot { it.id == id }
    }

    suspend fun markPickedUp(id: String) {
        val updated = api.updateReservation(id, ReservationStatus.PICKED_UP)
        reservations = reservations.map { if (it.id == updated.id) updated else it }
    }
}
