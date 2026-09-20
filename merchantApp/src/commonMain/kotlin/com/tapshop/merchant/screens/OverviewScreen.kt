package com.tapshop.merchant.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tapshop.merchant.LocalMerchantState
import com.tapshop.shared.model.AnalyticsSummary
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.components.BarChart
import com.tapshop.ui.components.BarDatum
import com.tapshop.ui.components.ErrorBanner
import com.tapshop.ui.components.HairlineDivider
import com.tapshop.ui.components.KpiTile
import com.tapshop.ui.components.LegendDot
import com.tapshop.ui.components.LineChart
import com.tapshop.ui.components.RemoteImage
import com.tapshop.ui.components.Skeleton
import com.tapshop.ui.components.StatusDot
import com.tapshop.ui.components.SurfaceCard
import com.tapshop.ui.theme.TapTheme

@Composable
fun OverviewScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalMerchantState.current
    val summary = state.summary

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            PageHeader(s.navOverview, s.overviewSubtitle, trailing = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    StatusDot(if (state.error == null) c.success else c.danger)
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.error == null) s.live else s.offline, style = MaterialTheme.typography.labelMedium, color = c.secondary)
                }
            })
        }
        state.error?.let { err ->
            item { ErrorBanner("${s.offline} ($err)", onRetry = { state.refresh() }, retryLabel = s.retry) }
        }
        if (summary == null) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    repeat(4) { Skeleton(Modifier.weight(1f).height(110.dp), radius = 22.dp) }
                }
            }
            item { Skeleton(Modifier.fillMaxWidth().height(280.dp), radius = 22.dp) }
        } else {
            item { KpiRow(summary) }
            item { ChartsRow(summary) }
            item { TopArticlesTable(summary) }
        }
    }
}

@Composable
private fun KpiRow(summary: AnalyticsSummary) {
    val s = TapTheme.strings
    BoxWithConstraints {
        val twoRows = maxWidth < 760.dp
        val tiles: List<@Composable (Modifier) -> Unit> = listOf(
            { m -> KpiTile(s.kpiScans, summary.totalScans.toString(), m, accent = true) },
            { m -> KpiTile(s.kpiSaves, summary.totalSaves.toString(), m) },
            { m -> KpiTile(s.kpiCart, summary.totalCartAdds.toString(), m) },
            { m -> KpiTile(s.kpiReservations, summary.totalReservations.toString(), m) },
            { m -> KpiTile(s.kpiShoppers, summary.activeShoppers.toString(), m) },
        )
        if (twoRows) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        row.forEach { tile -> tile(Modifier.weight(1f)) }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                tiles.forEach { tile -> tile(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun ChartsRow(summary: AnalyticsSummary) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    BoxWithConstraints {
        val stacked = maxWidth < 900.dp
        val byArticle: @Composable (Modifier) -> Unit = { m ->
            SurfaceCard(m, padding = PaddingValues(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.chartByArticle, style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                    LegendDot(c.onSurface, s.colScans)
                    Spacer(Modifier.width(12.dp))
                    LegendDot(c.accent, s.colSaves)
                }
                Spacer(Modifier.height(20.dp))
                BarChart(
                    summary.perArticle.take(6).map { BarDatum(it.name.split(' ').take(2).joinToString(" "), it.scans.toFloat(), it.saves.toFloat()) },
                    height = 190.dp,
                )
            }
        }
        val daily: @Composable (Modifier) -> Unit = { m ->
            SurfaceCard(m, padding = PaddingValues(22.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.chartDaily, style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.weight(1f))
                    LegendDot(c.accent, s.colScans)
                    Spacer(Modifier.width(12.dp))
                    LegendDot(c.onSurface, s.colSaves)
                }
                Spacer(Modifier.height(20.dp))
                LineChart(
                    series = summary.daily.map { it.scans.toFloat() },
                    secondSeries = summary.daily.map { it.saves.toFloat() },
                    labels = summary.daily.map { it.day },
                    height = 190.dp,
                )
            }
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                byArticle(Modifier.fillMaxWidth())
                daily(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                byArticle(Modifier.weight(1.2f))
                daily(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TopArticlesTable(summary: AnalyticsSummary) {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val api = LocalApi.current
    SurfaceCard(padding = PaddingValues(0.dp)) {
        Text(s.topArticles, style = MaterialTheme.typography.titleMedium, color = c.onSurface, modifier = Modifier.padding(start = 22.dp, top = 20.dp, bottom = 10.dp))
        TableHeader(
            listOf(
                Col(s.colArticle, 3f),
                Col(s.colScans, 1f, TextAlign.End),
                Col(s.colViews, 1f, TextAlign.End),
                Col(s.colSaves, 1f, TextAlign.End),
                Col(s.colCart, 1f, TextAlign.End),
                Col(s.colShares, 1f, TextAlign.End),
                Col(s.colSaveRate, 1f, TextAlign.End),
            ),
        )
        summary.perArticle.forEachIndexed { index, row ->
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(3f), verticalAlignment = Alignment.CenterVertically) {
                    RemoteImage(row.image?.let(api::imageUrl), row.name, Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(row.name, style = MaterialTheme.typography.bodyMedium, color = c.onSurface, maxLines = 1)
                        Text(row.brand, style = MaterialTheme.typography.labelSmall, color = c.secondary)
                    }
                }
                TCell(row.scans.toString(), 1f, TextAlign.End, bold = true)
                TCell(row.views.toString(), 1f, TextAlign.End)
                TCell(row.saves.toString(), 1f, TextAlign.End)
                TCell(row.cartAdds.toString(), 1f, TextAlign.End)
                TCell(row.shares.toString(), 1f, TextAlign.End)
                TCell(percent(row.saves, row.scans), 1f, TextAlign.End, color = c.accent)
            }
            if (index != summary.perArticle.lastIndex) HairlineDivider(inset = 16.dp)
        }
        Spacer(Modifier.height(8.dp))
    }
}
