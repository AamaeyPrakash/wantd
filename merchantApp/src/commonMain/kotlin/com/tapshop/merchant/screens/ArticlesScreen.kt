package com.tapshop.merchant.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tapshop.merchant.LocalMerchantState
import com.tapshop.shared.api.ApiRoutes
import com.tapshop.shared.model.Article
import com.tapshop.shared.model.ArticleStats
import com.tapshop.shared.model.ColorOption
import com.tapshop.shared.model.formatPrice
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.app.LocalToast
import com.tapshop.ui.components.Chip
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.ColorDot
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.PillButton
import com.tapshop.ui.components.PillStyle
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.components.TapTextField
import com.tapshop.ui.platform.copyToClipboard
import com.tapshop.ui.platform.openUrl
import com.tapshop.ui.platform.randomId
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch

private sealed class Sheet {
    data class Edit(val article: Article, val isNew: Boolean) : Sheet()
    data class Qr(val article: Article) : Sheet()
    data class ConfirmDelete(val article: Article) : Sheet()
}

@Composable
fun ArticlesScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalMerchantState.current
    val api = LocalApi.current
    val toast = LocalToast.current
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf<Sheet?>(null) }
    val stats = state.summary?.perArticle?.associateBy { it.articleId } ?: emptyMap()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(s.navArticles, "${state.articles.size} ${s.navArticles.lowercase()}", trailing = {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton(s.printSheet, onClick = { openUrl(api.qrSheetUrl()) }, style = PillStyle.Secondary, height = 44.dp)
                    PillButton(s.newArticle, icon = Icons.Rounded.Add, height = 44.dp, onClick = {
                        val store = state.stores.firstOrNull()
                        sheet = Sheet.Edit(
                            Article(
                                id = "a-" + randomId().take(6).lowercase(),
                                storeId = store?.id ?: "",
                                brand = store?.brand ?: "",
                                name = "",
                                category = "",
                                priceCents = 0,
                                description = "",
                                material = "",
                                stock = 10,
                            ),
                            isNew = true,
                        )
                    })
                }
            })
        }
        item {
            SurfaceCard(padding = PaddingValues(0.dp)) {
                TableHeader(
                    listOf(
                        Col(s.colArticle, 3f),
                        Col(s.fieldStore, 1.6f),
                        Col(s.colPrice, 1f, TextAlign.End),
                        Col(s.colStock, 0.8f, TextAlign.End),
                        Col(s.colScans, 0.8f, TextAlign.End),
                        Col(s.colSaves, 0.8f, TextAlign.End),
                        Col("", 1.8f, TextAlign.End),
                    ),
                )
                state.articles.forEachIndexed { index, a ->
                    ArticleTableRow(
                        a,
                        stats[a.id],
                        storeLabel = state.store(a.storeId)?.let { "${it.name} · ${it.location}" } ?: a.storeId,
                        onEdit = { sheet = Sheet.Edit(a, isNew = false) },
                        onQr = { sheet = Sheet.Qr(a) },
                        onDelete = { sheet = Sheet.ConfirmDelete(a) },
                    )
                    if (index != state.articles.lastIndex) HairlineDivider(inset = 16.dp)
                }
            }
        }
    }

    when (val current = sheet) {
        null -> Unit
        is Sheet.Edit -> EditArticleDialog(
            current.article,
            current.isNew,
            onDismiss = { sheet = null },
            onSave = { updated ->
                scope.launch {
                    runCatching { state.saveArticle(updated, current.isNew) }
                        .onSuccess { toast.show(s.saved); sheet = null }
                        .onFailure { toast.show(s.errorGeneric) }
                }
            },
        )
        is Sheet.Qr -> QrDialog(current.article, onDismiss = { sheet = null })
        is Sheet.ConfirmDelete -> ConfirmDialog(
            title = s.delete,
            message = s.deleteArticleConfirm,
            confirmLabel = s.delete,
            onDismiss = { sheet = null },
            onConfirm = {
                scope.launch {
                    runCatching { state.deleteArticle(current.article.id) }
                        .onFailure { toast.show(s.errorGeneric) }
                    sheet = null
                }
            },
        )
    }
}

@Composable
private fun ArticleTableRow(
    a: Article,
    stats: ArticleStats?,
    storeLabel: String,
    onEdit: () -> Unit,
    onQr: () -> Unit,
    onDelete: () -> Unit,
) {
    val api = LocalApi.current
    val c = TapTheme.colors
    val s = TapTheme.strings
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(3f), verticalAlignment = Alignment.CenterVertically) {
            RemoteImage(a.images.firstOrNull()?.let(api::imageUrl), a.name, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(a.name, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, maxLines = 1)
                Text("${a.brand} · ${a.category}", style = MaterialTheme.typography.labelSmall, color = c.secondary, maxLines = 1)
            }
        }
        TCell(storeLabel, 1.6f, color = c.secondary)
        TCell(formatPrice(a.priceCents, a.currency), 1f, TextAlign.End, bold = true)
        TCell(a.stock.toString(), 0.8f, TextAlign.End, color = if (a.stock <= 5) c.danger else c.onSurface)
        TCell(stats?.scans?.toString() ?: "–", 0.8f, TextAlign.End)
        TCell(stats?.saves?.toString() ?: "–", 0.8f, TextAlign.End)
        Row(Modifier.weight(1.8f), horizontalArrangement = Arrangement.End) {
            CircleIconButton(Icons.Rounded.Share, s.showQr, onClick = onQr, size = 36.dp, background = c.surfaceVariant)
            Spacer(Modifier.width(6.dp))
            CircleIconButton(Icons.Rounded.Edit, s.edit, onClick = onEdit, size = 36.dp, background = c.surfaceVariant)
            Spacer(Modifier.width(6.dp))
            CircleIconButton(Icons.Rounded.Delete, s.delete, onClick = onDelete, size = 36.dp, background = c.surfaceVariant, tint = c.danger)
        }
    }
}

@Composable
private fun DialogFrame(title: String, onDismiss: () -> Unit, maxWidth: Int = 640, content: @Composable () -> Unit) {
    val c = TapTheme.colors
    val s = TapTheme.strings
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        SurfaceCard(
            Modifier.widthIn(max = maxWidth.dp).padding(16.dp),
            radius = 28.dp,
            padding = PaddingValues(24.dp),
            background = c.surfaceElevated,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.headlineSmall, color = c.onSurface, modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Rounded.Close, s.close, onClick = onDismiss, size = 36.dp, background = c.surfaceVariant)
            }
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
private fun EditArticleDialog(initial: Article, isNew: Boolean, onDismiss: () -> Unit, onSave: (Article) -> Unit) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalMerchantState.current

    var name by remember { mutableStateOf(initial.name) }
    var brand by remember { mutableStateOf(initial.brand) }
    var category by remember { mutableStateOf(initial.category) }
    var price by remember { mutableStateOf(if (initial.priceCents == 0L) "" else (initial.priceCents / 100.0).toString()) }
    var stock by remember { mutableStateOf(initial.stock.toString()) }
    var material by remember { mutableStateOf(initial.material) }
    var care by remember { mutableStateOf(initial.care) }
    var description by remember { mutableStateOf(initial.description) }
    var sizes by remember { mutableStateOf(initial.sizes.joinToString(", ")) }
    var colors by remember { mutableStateOf(initial.colors.joinToString(", ") { "${it.name}:${it.hex}" }) }
    var tags by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var storeId by remember { mutableStateOf(initial.storeId) }

    val parsedColors = remember(colors) { parseColors(colors) }
    val valid = name.isNotBlank() && brand.isNotBlank() && price.toDoubleOrNull() != null && stock.toIntOrNull() != null && storeId.isNotBlank()

    DialogFrame(if (isNew) s.newArticle else s.editArticle, onDismiss) {
        Column(
            Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TapTextField(name, { name = it }, Modifier.weight(2f), label = s.fieldName)
                TapTextField(brand, { brand = it }, Modifier.weight(1f), label = s.fieldBrand)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TapTextField(category, { category = it }, Modifier.weight(1f), label = s.fieldCategory)
                TapTextField(price, { price = it.filter { ch -> ch.isDigit() || ch == '.' } }, Modifier.weight(1f), label = "${s.fieldPrice} (${initial.currency})")
                TapTextField(stock, { stock = it.filter(Char::isDigit) }, Modifier.weight(0.7f), label = s.fieldStock)
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(s.fieldStore.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary, modifier = Modifier.padding(start = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.stores.forEach { store ->
                        Chip("${store.brand} · ${store.name}", selected = store.id == storeId, onClick = { storeId = store.id })
                    }
                }
            }
            TapTextField(material, { material = it }, Modifier.fillMaxWidth(), label = s.fieldMaterial)
            TapTextField(care, { care = it }, Modifier.fillMaxWidth(), label = s.fieldCare)
            TapTextField(description, { description = it }, Modifier.fillMaxWidth(), label = s.fieldDescription, singleLine = false, minLines = 3)
            TapTextField(sizes, { sizes = it }, Modifier.fillMaxWidth(), label = s.fieldSizes)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TapTextField(colors, { colors = it }, Modifier.fillMaxWidth(), label = s.fieldColors)
                if (parsedColors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 4.dp)) {
                        parsedColors.forEach { ColorDot(parseHex(it.hex), selected = false, size = 22.dp) }
                    }
                }
            }
            TapTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = s.fieldTags)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            PillButton(s.cancel, onClick = onDismiss, style = PillStyle.Ghost)
            PillButton(if (isNew) s.add else s.saveChanges, enabled = valid, onClick = {
                onSave(
                    initial.copy(
                        name = name.trim(),
                        brand = brand.trim(),
                        category = category.trim(),
                        priceCents = ((price.toDoubleOrNull() ?: 0.0) * 100).toLong(),
                        stock = stock.toIntOrNull() ?: 0,
                        material = material.trim(),
                        care = care.trim(),
                        description = description.trim(),
                        sizes = sizes.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        colors = parsedColors,
                        tags = tags.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        storeId = storeId,
                        images = initial.images.ifEmpty { listOf("${initial.id}.jpg") },
                    ),
                )
            })
        }
    }
}

@Composable
private fun QrDialog(a: Article, onDismiss: () -> Unit) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val api = LocalApi.current
    val toast = LocalToast.current
    val state = LocalMerchantState.current
    val publicBase = state.serverInfo?.publicBaseUrl ?: api.baseUrl
    val tagUrl = ApiRoutes.tagUrl(publicBase, a.id)

    DialogFrame("${s.qrCode} · ${a.name}", onDismiss, maxWidth = 520) {
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Box(Modifier.size(220.dp).clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Color.White).padding(10.dp)) {
                RemoteImage(api.qrUrl(a.id), s.qrCode, Modifier.fillMaxSize(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
            }
            Column(Modifier.weight(1f)) {
                Text(s.qrHint, style = MaterialTheme.typography.bodySmall, color = c.secondary)
                Spacer(Modifier.height(14.dp))
                Text(s.tagUrl.uppercase(), style = MaterialTheme.typography.labelSmall, color = c.secondary)
                Spacer(Modifier.height(4.dp))
                Text(tagUrl, style = MaterialTheme.typography.bodySmall, color = c.onSurface)
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton(s.copy, style = PillStyle.Secondary, height = 40.dp, contentPadding = 16.dp, onClick = {
                        copyToClipboard(tagUrl)
                        toast.show(s.copied)
                    })
                    PillButton(s.printSheet, style = PillStyle.Ghost, height = 40.dp, contentPadding = 16.dp, onClick = { openUrl(api.qrSheetUrl()) })
                }
            }
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    DialogFrame(title, onDismiss, maxWidth = 440) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = c.onSurface)
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.weight(1f))
            PillButton(s.cancel, onClick = onDismiss, style = PillStyle.Ghost)
            PillButton(confirmLabel, onClick = onConfirm, style = PillStyle.Danger)
        }
    }
}

private fun parseColors(raw: String): List<ColorOption> = raw.split(',').mapNotNull { part ->
    val trimmed = part.trim()
    if (trimmed.isEmpty()) return@mapNotNull null
    val name = trimmed.substringBefore(':').trim()
    val hex = trimmed.substringAfter(':', "").trim().ifEmpty { "#888888" }
    if (name.isEmpty()) null else ColorOption(name, if (hex.startsWith("#")) hex else "#$hex")
}

private fun parseHex(hex: String): androidx.compose.ui.graphics.Color = try {
    val clean = hex.removePrefix("#")
    val value = clean.toLong(16)
    if (clean.length == 6) androidx.compose.ui.graphics.Color((0xFF000000L or value).toInt()) else androidx.compose.ui.graphics.Color(value.toInt())
} catch (_: Throwable) {
    androidx.compose.ui.graphics.Color.Gray
}
