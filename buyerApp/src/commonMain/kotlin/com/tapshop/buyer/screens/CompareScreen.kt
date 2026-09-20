package com.tapshop.buyer.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.buyer.LocalNavigator
import com.tapshop.buyer.Screen
import com.tapshop.shared.i18n.fmt
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ChatMessage
import com.tapshop.shared.model.CompareRequest
import com.tapshop.shared.model.CompareResult
import com.tapshop.shared.model.EventType
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.ErrorBanner
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.ScoreBar
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.components.TapTextField
import com.tapshop.ui.components.ThinkingDots
import com.tapshop.ui.platform.decodeImage
import com.tapshop.ui.platform.pickImagesAsDataUrls
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
    var question by remember { mutableStateOf("") }
    var photos by remember { mutableStateOf<List<String>>(emptyList()) }
    var result by remember { mutableStateOf<CompareResult?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun run() {
        scope.launch {
            loading = true
            error = null
            try {
                result = api.compare(
                    CompareRequest(
                        uid = api.uid,
                        articleIds = ids,
                        userImagesBase64 = photos,
                        question = question.ifBlank { null },
                        language = settings.language.code,
                    ),
                )
                ids.forEach { state.track(it, EventType.COMPARE) }
            } catch (t: Throwable) {
                error = t.message ?: s.compareError
            } finally {
                loading = false
            }
        }
    }

    // Kick off automatically the first time so the verdict appears without an extra tap.
    LaunchedEffect(ids) { if (result == null && !loading && articles.size >= 2) run() }

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
                    Text(s.compareAddPhoto, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Text(s.compareAddPhotoHint, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        photos.forEach { dataUrl -> PhotoThumb(dataUrl) }
                        if (photos.size < 3) {
                            Box(
                                Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(c.surfaceVariant)
                                    .border(1.dp, c.separator, RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircleIconButton(Icons.Rounded.Add, s.compareAddPhoto, background = c.surfaceVariant, onClick = {
                                    scope.launch {
                                        val picked = pickImagesAsDataUrls(3 - photos.size)
                                        if (picked.isNotEmpty()) photos = photos + picked
                                    }
                                })
                            }
                        }
                        if (photos.isNotEmpty()) {
                            Column {
                                Text(s.comparePhotosAdded.fmt(photos.size), style = MaterialTheme.typography.labelMedium, color = c.onSurface)
                                Text(
                                    s.compareRemovePhotos,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = c.accent,
                                    modifier = Modifier.clickable { photos = emptyList() }.padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    TapTextField(
                        value = question,
                        onValueChange = { question = it },
                        label = s.compareQuestion,
                        placeholder = s.compareQuestionPlaceholder,
                        modifier = Modifier.fillMaxWidth(),
                        onSubmit = { if (!loading) run() },
                    )
                    Spacer(Modifier.height(14.dp))
                    PillButton(
                        text = if (result == null) s.compareRun else s.compareRerun,
                        icon = SparkleIcon,
                        loading = loading,
                        enabled = articles.size >= 2,
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
                if (r.criteria.isNotEmpty()) {
                    item {
                        SurfaceCard {
                            Text(s.compareBreakdown, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
                            Spacer(Modifier.height(8.dp))
                            r.criteria.forEachIndexed { index, criterion ->
                                Spacer(Modifier.height(10.dp))
                                Text(criterion.name, style = MaterialTheme.typography.labelLarge, color = c.onSurface)
                                Spacer(Modifier.height(8.dp))
                                val best = criterion.scores.maxByOrNull { it.value }?.key
                                articles.forEach { a ->
                                    val score = criterion.scores[a.id] ?: 0
                                    ScoreBar(a.name, score, highlight = a.id == best, modifier = Modifier.padding(vertical = 3.dp))
                                }
                                if (criterion.note.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(criterion.note, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                                }
                                if (index != r.criteria.lastIndex) {
                                    Spacer(Modifier.height(12.dp))
                                    HairlineDivider()
                                }
                            }
                        }
                    }
                }
                if (r.recommendation.isNotBlank()) {
                    item {
                        SurfaceCard(background = c.accentSoft) {
                            Text(s.compareRecommendation, style = MaterialTheme.typography.labelMedium, color = c.accent)
                            Spacer(Modifier.height(6.dp))
                            Text(r.recommendation, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
                        }
                    }
                }
                item {
                    PillButton(
                        text = s.compareAskFollowUp,
                        icon = SparkleIcon,
                        style = PillStyle.Secondary,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            // Seed the chat with the verdict so the agent has context.
                            if (state.chat.isEmpty()) {
                                state.chat.add(ChatMessage("assistant", r.summary))
                            }
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
private fun PhotoThumb(dataUrl: String) {
    val bitmap: ImageBitmap? = remember(dataUrl) {
        runCatching { decodeImage(decodeDataUrl(dataUrl)) }.getOrNull()
    }
    Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(TapTheme.colors.surfaceVariant)) {
        if (bitmap != null) {
            Image(bitmap, null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        }
    }
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
            Text(s.compareVerdict, style = MaterialTheme.typography.titleMedium, color = c.onSurface)
        }
        Spacer(Modifier.height(12.dp))
        Text(r.summary, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
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

/** Strips the `data:*;base64,` prefix and decodes the payload. */
internal fun decodeDataUrl(dataUrl: String): ByteArray {
    val payload = dataUrl.substringAfter("base64,", dataUrl)
    return base64Decode(payload)
}

private fun base64Decode(input: String): ByteArray {
    val table = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
    val clean = input.filter { it != '=' && !it.isWhitespace() }
    val out = ArrayList<Byte>(clean.length * 3 / 4)
    var buffer = 0
    var bits = 0
    for (ch in clean) {
        val v = table.indexOf(ch)
        if (v < 0) continue
        buffer = (buffer shl 6) or v
        bits += 6
        if (bits >= 8) {
            bits -= 8
            out.add(((buffer shr bits) and 0xFF).toByte())
        }
    }
    return out.toByteArray()
}
