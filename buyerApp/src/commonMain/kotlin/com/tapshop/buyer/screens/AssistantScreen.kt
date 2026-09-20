package com.tapshop.buyer.screens

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
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
import androidx.compose.ui.unit.dp
import com.tapshop.buyer.LocalBuyerState
import com.tapshop.shared.model.ChatMessage
import com.tapshop.shared.model.ChatRequest
import com.tapshop.ui.app.LocalApi
import com.tapshop.ui.components.Chip
import com.tapshop.ui.components.CircleIconButton
import com.tapshop.ui.components.ErrorBanner
import com.tapshop.ui.components.LargeTitle
import com.tapshop.ui.components.SparkleIcon
import com.tapshop.ui.components.TapTextField
import com.tapshop.ui.components.ThinkingDots
import com.tapshop.ui.settings.LocalAppSettings
import com.tapshop.ui.theme.TapTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

@Composable
fun AssistantScreen() {
    val s = TapTheme.strings
    val c = TapTheme.colors
    val state = LocalBuyerState.current
    val api = LocalApi.current
    val settings = LocalAppSettings.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val messages = state.chat

    fun send(text: String, retry: Boolean = false) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || thinking) return
        input = ""
        error = null
        if (!retry) messages.add(ChatMessage("user", trimmed))
        scope.launch {
            thinking = true
            try {
                val response = api.chat(ChatRequest(uid = api.uid, messages = messages.toList(), language = settings.language.code))
                messages.add(ChatMessage("assistant", response.reply))
                // Tools may have changed the cart (e.g. "add it to my cart"), so refresh quietly.
                state.refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                error = s.compareError
            } finally {
                thinking = false
            }
        }
    }

    LaunchedEffect(messages.size, thinking) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size + 1)
    }

    ReadableColumn { Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(c.pill), contentAlignment = Alignment.Center) {
                Icon(SparkleIcon, null, tint = c.onPill, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            LargeTitle(s.assistantTitle)
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Text(s.assistantIntro, style = MaterialTheme.typography.bodyMedium, color = c.secondary) }
            if (messages.isEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 6.dp)) {
                        listOf(s.assistantSuggestion1, s.assistantSuggestion2, s.assistantSuggestion3).forEach { suggestion ->
                            Chip(suggestion, onClick = { send(suggestion) }, icon = SparkleIcon)
                        }
                    }
                }
            }
            messages.forEach { m -> item { Bubble(m) } }
            error?.let { message ->
                item {
                    ErrorBanner(message, retryLabel = s.retry, onRetry = {
                        messages.lastOrNull { it.role == "user" }?.let { send(it.content, retry = true) }
                    })
                }
            }
            if (thinking) {
                item {
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(c.surface).padding(horizontal = 16.dp, vertical = 14.dp)) { ThinkingDots() }
                }
            }
            item { Spacer(Modifier.height(4.dp)) }
        }

        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = s.assistantPlaceholder,
                modifier = Modifier.weight(1f),
                onSubmit = { send(input) },
            )
            Spacer(Modifier.width(10.dp))
            CircleIconButton(
                Icons.AutoMirrored.Rounded.Send,
                s.assistantSend,
                size = 48.dp,
                background = c.pill,
                tint = c.onPill,
                enabled = input.isNotBlank() && !thinking,
                onClick = { send(input) },
            )
        }
    } }
}

@Composable
private fun Bubble(message: ChatMessage) {
    val c = TapTheme.colors
    val isUser = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (isUser) 20.dp else 6.dp,
                        bottomEnd = if (isUser) 6.dp else 20.dp,
                    ),
                )
                .background(if (isUser) c.pill else c.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(
                message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUser) c.onPill else c.onSurface,
            )
        }
    }
}
