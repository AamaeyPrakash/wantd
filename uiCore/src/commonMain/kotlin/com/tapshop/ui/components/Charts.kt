package com.tapshop.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.theme.TapTheme

data class BarDatum(val label: String, val value: Float, val secondary: Float? = null)

/** Vertical bar chart (optionally two series per label). */
@Composable
fun BarChart(
    data: List<BarDatum>,
    modifier: Modifier = Modifier,
    height: Dp = 180.dp,
    primaryColor: Color = TapTheme.colors.onSurface,
    secondaryColor: Color = TapTheme.colors.accent,
    showValues: Boolean = true,
) {
    val c = TapTheme.colors
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(if (appeared) 1f else 0f, tween(700))
    val maxValue = (data.maxOfOrNull { maxOf(it.value, it.secondary ?: 0f) } ?: 1f).coerceAtLeast(1f)

    Column(modifier) {
        Row(Modifier.fillMaxWidth().height(height), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data.forEach { d ->
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                    if (showValues) {
                        Text(
                            d.value.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = c.secondary,
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    Canvas(Modifier.fillMaxWidth().weight(1f)) {
                        val gap = 4.dp.toPx()
                        val hasSecondary = d.secondary != null
                        val barWidth = if (hasSecondary) (size.width - gap) / 2 else size.width * 0.72f
                        val startX = if (hasSecondary) 0f else (size.width - barWidth) / 2
                        val h1 = size.height * (d.value / maxValue) * progress
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(startX, size.height - h1),
                            size = Size(barWidth, h1),
                            cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                        )
                        if (hasSecondary) {
                            val h2 = size.height * ((d.secondary ?: 0f) / maxValue) * progress
                            drawRoundRect(
                                color = secondaryColor,
                                topLeft = Offset(startX + barWidth + gap, size.height - h2),
                                size = Size(barWidth, h2),
                                cornerRadius = CornerRadius(barWidth / 2.5f, barWidth / 2.5f),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            data.forEach { d ->
                Text(
                    d.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.secondary,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Smooth line with gradient fill; up to two series. */
@Composable
fun LineChart(
    series: List<Float>,
    modifier: Modifier = Modifier,
    secondSeries: List<Float>? = null,
    labels: List<String> = emptyList(),
    height: Dp = 160.dp,
    color: Color = TapTheme.colors.accent,
    secondColor: Color = TapTheme.colors.onSurface,
) {
    val c = TapTheme.colors
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(if (appeared) 1f else 0f, tween(900))
    val all = series + (secondSeries ?: emptyList())
    val maxValue = (all.maxOrNull() ?: 1f).coerceAtLeast(1f)

    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val w = size.width
            val h = size.height
            val stepY = h / 4
            for (i in 0..4) {
                drawLine(c.separator, Offset(0f, i * stepY), Offset(w, i * stepY), strokeWidth = 1f)
            }
            fun points(values: List<Float>): List<Offset> {
                if (values.size < 2) return emptyList()
                val dx = w / (values.size - 1)
                return values.mapIndexed { i, v -> Offset(i * dx, h - (v / maxValue) * h * 0.92f * progress - h * 0.04f) }
            }
            fun smooth(points: List<Offset>): Path {
                val p = Path()
                if (points.isEmpty()) return p
                p.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    val prev = points[i - 1]
                    val cur = points[i]
                    val midX = (prev.x + cur.x) / 2
                    p.cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                }
                return p
            }
            secondSeries?.let { s ->
                val pts = points(s)
                if (pts.isNotEmpty()) {
                    drawPath(smooth(pts), secondColor.copy(alpha = 0.55f), style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            }
            val pts = points(series)
            if (pts.isNotEmpty()) {
                val line = smooth(pts)
                val fill = Path().apply {
                    addPath(line)
                    lineTo(pts.last().x, h)
                    lineTo(pts.first().x, h)
                    close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), color.copy(alpha = 0f)), endY = h))
                drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val last = pts.last()
                drawCircle(c.surface, radius = 6.dp.toPx(), center = last)
                drawCircle(color, radius = 4.dp.toPx(), center = last)
            }
        }
        if (labels.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = c.secondary) }
            }
        }
    }
}

/** Horizontal 1-10 score bar used in AI comparison rows. */
@Composable
fun ScoreBar(
    label: String,
    score: Int,
    modifier: Modifier = Modifier,
    max: Int = 10,
    color: Color = TapTheme.colors.accent,
    highlight: Boolean = false,
) {
    val c = TapTheme.colors
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val progress by animateFloatAsState(if (appeared) score.toFloat() / max else 0f, tween(600))
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) c.onSurface else c.secondary,
            modifier = Modifier.width(96.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(c.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(progress).height(8.dp).clip(CircleShape).background(if (highlight) color else c.tertiary))
        }
        Spacer(Modifier.width(10.dp))
        Text(score.toString(), style = MaterialTheme.typography.labelLarge, color = c.onSurface, modifier = Modifier.width(22.dp), textAlign = TextAlign.End)
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TapTheme.colors.secondary)
    }
}

@Composable
fun KpiTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    delta: String? = null,
    accent: Boolean = false,
) {
    val c = TapTheme.colors
    SurfaceCard(modifier, padding = androidx.compose.foundation.layout.PaddingValues(18.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(10.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = if (accent) c.accent else c.onSurface)
        if (delta != null) {
            Spacer(Modifier.height(4.dp))
            Text(delta, style = MaterialTheme.typography.labelSmall, color = c.success)
        }
    }
}
