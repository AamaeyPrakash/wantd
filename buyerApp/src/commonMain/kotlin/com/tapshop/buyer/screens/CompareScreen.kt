package com.tapshop.buyer.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.Screen
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ChatMessage
import com.tapshop.shared.model.ColorPreference
import com.tapshop.shared.model.FitPreference
import com.tapshop.shared.model.OccasionPreference
import com.tapshop.shared.model.ShoppingPriority
import com.tapshop.shared.model.ComparePreferences
import com.tapshop.shared.model.CompareRequest
import com.tapshop.shared.model.CompareResult
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.components.Chip
import com.tapshop.ui.components.ErrorBanner
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.components.ThinkingDots
import com.tapshop.ui.settings.LocalAppSettings
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

@Composable
fun CompareScreen(ids: List<String>) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val nav = LocalNavigator.current
    val api = LocalApi.current
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()

    val articles = remember(ids, state.articles) { ids.mapNotNull { state.article(it) } }
    var color by remember(ids) { mutableStateOf<ColorPreference?>(null) }
    var fit by remember(ids) { mutableStateOf<FitPreference?>(null) }
    var occasion by remember(ids) { mutableStateOf<OccasionPreference?>(null) }
    var priority by remember(ids) { mutableStateOf<ShoppingPriority?>(null) }
    val colorOptions = listOf(
        ColorPreference.NEUTRAL to s.prefColorNeutral, ColorPreference.DARK to s.prefColorDark,
        ColorPreference.LIGHT to s.prefColorLight, ColorPreference.BOLD to s.prefColorBold,
        ColorPreference.ANY to s.prefAny,
    )
    val fitOptions = listOf(
        FitPreference.RELAXED to s.prefFitRelaxed, FitPreference.REGULAR to s.prefFitRegular,
        FitPreference.TAILORED to s.prefFitTailored, FitPreference.OVERSIZED to s.prefFitOversized,
        FitPreference.ANY to s.prefAny,
    )
    val occasionOptions = listOf(
        OccasionPreference.EVERYDAY to s.prefOccasionEveryday, OccasionPreference.WORK to s.prefOccasionWork,
        OccasionPreference.EVENING to s.prefOccasionEvening, OccasionPreference.TRAVEL to s.prefOccasionTravel,
        OccasionPreference.ANY to s.prefAny,
    )
    val priorityOptions = listOf(
        ShoppingPriority.PRICE to s.prefPriorityPrice, ShoppingPriority.QUALITY to s.prefPriorityQuality,
        ShoppingPriority.VERSATILITY to s.prefPriorityVersatility, ShoppingPriority.COMFORT to s.prefPriorityComfort,
        ShoppingPriority.ANY to s.prefAny,
    )
    val preferences = color?.let { selectedColor ->
        fit?.let { selectedFit ->
            occasion?.let { selectedOccasion ->
                priority?.let { ComparePreferences(selectedColor, selectedFit, selectedOccasion, it) }
            }
        }
    }
    val validPair = ids.size == 2 && ids.distinct().size == 2 && articles.size == 2
    var result by remember(ids) { mutableStateOf<CompareResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run() {
        val answers = preferences ?: return
        if (loading || !validPair) return
        scope.launch {
            loading = true
            error = null
            result = null
            try {
                result = api.compare(
                    CompareRequest(
                        uid = api.uid,
                        articleIds = ids,
                        preferences = answers,
                        language = settings.language.code,
                    ),
                )
            } catch (t: Throwable) {
                error = t.message ?: s.compareError
            } finally {
                loading = false
            }
        }
    }

    ReadableColumn { Column(Modifier.fillMaxSize()) {
        ScreenHeader(s.compareTitle)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(articles, key = { it.id }) { a ->
                        ArticleTile(a, winner = result?.winnerArticleId == a.id) { nav.push(Screen.Article(a.id)) }
                    }
                }
            }

            item {
                SurfaceCard {
                    Text(s.compareIntro, style = MaterialTheme.typography.bodyMedium, color = c.secondary)
                    Spacer(Modifier.height(16.dp))
                    PreferenceQuestion(s.prefColorTitle, colorOptions, color, !loading) { color = it; result = null; error = null }
                    PreferenceQuestion(s.prefFitTitle, fitOptions, fit, !loading) { fit = it; result = null; error = null }
                    PreferenceQuestion(s.prefOccasionTitle, occasionOptions, occasion, !loading) { occasion = it; result = null; error = null }
                    PreferenceQuestion(s.prefPriorityTitle, priorityOptions, priority, !loading) { priority = it; result = null; error = null }
                    if (!validPair) {
                        Text(s.selectAtLeastTwo, style = MaterialTheme.typography.bodySmall, color = c.danger)
                        Spacer(Modifier.height(12.dp))
                    }
                    PillButton(
                        text = if (result == null) s.compareRun else s.compareRerun,
                        icon = SparkleIcon,
                        loading = loading,
                        enabled = validPair && preferences != null,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { run() },
                    )
                }
            }

            if (loading) {
                item {
                    SurfaceCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ThinkingDots()
                            Spacer(Modifier.width(12.dp))
                            Text(s.compareThinking, style = MaterialTheme.typography.bodyMedium, color = c.secondary)
                        }
                    }
                }
            }
            error?.let { err ->
                item { ErrorBanner("${s.compareError} ($err)", onRetry = { run() }, retryLabel = s.retry) }
            }

            result?.let { r ->
                item { VerdictCard(r, articles) }
                item {
                    PillButton(
                        text = s.compareAskFollowUp,
                        icon = SparkleIcon,
                        style = PillStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Keep the selected pair and answers available for follow-up questions.
                            val context = listOf(
                                "${s.compareTitle}: ${articles.joinToString { it.name }}",
                                "${s.prefColorTitle}: ${colorOptions.first { it.first == color }.second}",
                                "${s.prefFitTitle}: ${fitOptions.first { it.first == fit }.second}",
                                "${s.prefOccasionTitle}: ${occasionOptions.first { it.first == occasion }.second}",
                                "${s.prefPriorityTitle}: ${priorityOptions.first { it.first == priority }.second}",
                            ).joinToString("\n")
                            state.chat.add(ChatMessage("user", context))
                            state.chat.add(ChatMessage("assistant", "${r.summary}\n${r.recommendation}"))
                            nav.reset(Screen.Assistant)
                        },
                    )
                }
            }
        }
    } }
}

@Composable
private fun ArticleTile(a: Article, winner: Boolean, onClick: () -> Unit) {
    val api = LocalApi.current
    val c = TapTheme.colors
    Column(Modifier.width(120.dp)) {
        Box {
            RemoteImage(
                url = a.images.firstOrNull()?.let(api::imageUrl),
                contentDescription = a.name,
                modifier = Modifier.size(120.dp).clip(RoundedCornerShape(20.dp))
                    .then(if (winner) Modifier.border(2.dp, c.accent, RoundedCornerShape(20.dp)) else Modifier)
                    .clickable(onClick = onClick),
            )
            if (winner) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(8.dp).size(24.dp).clip(CircleShape).background(c.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Star, null, tint = c.onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(a.name, style = MaterialTheme.typography.labelMedium, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(formatPrice(a.priceCents, a.currency), style = MaterialTheme.typography.labelSmall, color = c.secondary)
    }
}

@Composable
private fun <T> PreferenceQuestion(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    enabled: Boolean,
    onSelect: (T) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = TapTheme.colors.onSurface)
    Spacer(Modifier.height(8.dp))
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            Chip(label, selected = value == selected, onClick = if (enabled) ({ onSelect(value) }) else null)
        }
    }
    Spacer(Modifier.height(18.dp))
}

@Composable
private fun VerdictCard(r: CompareResult, articles: List<Article>) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val api = LocalApi.current
    val winner = articles.firstOrNull { it.id == r.winnerArticleId }
    SurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(28.dp).clip(CircleShape).background(c.pill), contentAlignment = Alignment.Center) {
                Icon(SparkleIcon, null, tint = c.onPill, modifier = Modifier.size(15.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(r.summary, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Text(r.recommendation, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
        if (winner != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(c.surfaceVariant).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RemoteImage(
                    url = winner.images.firstOrNull()?.let(api::imageUrl),
                    contentDescription = winner.name,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(s.compareBestPick.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.accent)
                    Text(winner.name, style = MaterialTheme.typography.titleSmall, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${winner.brand} · ${formatPrice(winner.priceCents, winner.currency)}", style = MaterialTheme.typography.bodySmall, color = c.secondary)
                }
                Icon(Icons.Rounded.Check, null, tint = c.success, modifier = Modifier.size(22.dp))
            }
        }
    }
}
