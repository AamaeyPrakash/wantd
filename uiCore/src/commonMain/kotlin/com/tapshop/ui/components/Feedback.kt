package com.tapshop.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.delay

/** Simple toast host: call [ToastState.show] from anywhere in the tree. */
class ToastState {
    var message by mutableStateOf<String?>(null)
        private set
    var icon by mutableStateOf<ImageVector?>(null)
        private set
    private var token = 0

    fun show(text: String, icon: ImageVector? = null) {
        message = text
        this.icon = icon
        token++
    }

    internal fun currentToken() = token
    internal fun clear() { message = null }
}

@Composable
fun BoxScope.ToastHost(state: ToastState, modifier: Modifier = Modifier, bottomPadding: Dp = 96.dp) {
    val c = TapTheme.colors
    val message = state.message
    LaunchedEffect(state.currentToken()) {
        if (state.message != null) {
            delay(2200)
            state.clear()
        }
    }
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier.align(Alignment.BottomCenter).padding(bottom = bottomPadding),
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Row(
            Modifier
                .widthIn(max = 360.dp)
                .clip(CircleShape)
                .background(c.pill)
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            state.icon?.let {
                Icon(it, null, tint = c.onPill, modifier = Modifier.size(16.dp))
                HorizontalGap(8.dp)
            }
            Text(message ?: "", style = MaterialTheme.typography.labelLarge, color = c.onPill)
        }
    }
}

/** Shimmering placeholder block. */
@Composable
fun Skeleton(modifier: Modifier = Modifier, radius: Dp = 14.dp) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(0.45f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse))
    Box(
        modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(radius))
            .alpha(alpha)
            .background(TapTheme.colors.surfaceVariant),
    )
}

/** Three pulsing dots while the assistant thinks. */
@Composable
fun ThinkingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition()
    val c = TapTheme.colors
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val a by transition.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(600, delayMillis = i * 180), RepeatMode.Reverse),
            )
            Box(Modifier.padding(horizontal = 3.dp).size(8.dp).clip(CircleShape).alpha(a).background(c.accent))
        }
    }
}

@Composable
fun ErrorBanner(text: String, modifier: Modifier = Modifier, onRetry: (() -> Unit)? = null, retryLabel: String = "Retry") {
    val c = TapTheme.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(c.danger.copy(alpha = 0.1f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.danger, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            HorizontalGap(12.dp)
            Text(
                retryLabel,
                style = MaterialTheme.typography.labelLarge,
                color = c.danger,
                modifier = Modifier.clickable { onRetry() },
            )
        }
    }
}
