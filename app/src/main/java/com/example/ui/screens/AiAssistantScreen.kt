package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.core.AppStrings
import com.example.core.FormatUtils
import com.example.ui.components.StaggeredEntranceCard
import com.example.ui.theme.CharcoalCard
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonBg
import com.example.ui.theme.CrimsonDown
import com.example.ui.theme.GoldBorder
import com.example.ui.theme.GoldBright
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.GoldSubtleBg
import com.example.ui.theme.MonoMetricMedium
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.theme.ObsidianBlack
import com.example.ui.theme.TextIvory
import com.example.ui.theme.TextMuted
import com.example.viewmodel.AdBoostDialogState
import com.example.viewmodel.AiUiState
import com.example.viewmodel.ChatMessage
import com.example.viewmodel.MiningUiState

@Composable
fun AiAssistantScreen(
    miningState: MiningUiState,
    chatMessages: List<ChatMessage>,
    aiUiState: AiUiState,
    adBoostDialogState: AdBoostDialogState,
    strings: AppStrings,
    onStartAiBoost: () -> Unit,
    onCancelAdBoost: () -> Unit,
    onSendMessage: (String) -> Unit,
    onRetryLast: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val isBusy = aiUiState is AiUiState.Loading || aiUiState is AiUiState.Streaming

    LaunchedEffect(chatMessages.size, aiUiState) {
        if (chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(chatMessages.size - 1)
        }
    }

    if (adBoostDialogState.isVisible) {
        SimulatedAdBoostDialog(
            remainingSeconds = adBoostDialogState.remainingSeconds,
            strings = strings,
            onCancel = onCancelAdBoost
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            // AI Boost 10-minute Hero Card
            StaggeredEntranceCard(index = 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(GoldSubtleBg, CharcoalElevated)
                            )
                        )
                        .border(1.dp, GoldPrimary.copy(alpha = 0.55f), RoundedCornerShape(24.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(GoldPrimary.copy(alpha = 0.14f))
                                .border(0.8.dp, GoldBorder, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = strings.aiBoostTitle,
                                tint = GoldPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = strings.aiBoostTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = GoldBright
                            )
                            Text(
                                text = strings.aiBoostDesc,
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Button(
                        onClick = onStartAiBoost,
                        enabled = !miningState.isBoostActive,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldPrimary,
                            contentColor = ObsidianBlack,
                            disabledContainerColor = CharcoalCard,
                            disabledContentColor = GoldPrimary
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("ai_boost_button")
                    ) {
                        if (miningState.isBoostActive) {
                            Text(
                                text = FormatUtils.formatCountdown(miningState.boostRemainingSeconds),
                                style = MonoMetricSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.PlayCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = strings.aiBoostButton,
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI HAIGENX1 Identity Strip
            StaggeredEntranceCard(index = 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(CharcoalCard)
                        .border(1.dp, GoldBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_ai_core_1794980916299),
                        contentDescription = strings.aiTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .border(1.dp, GoldPrimary, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.aiTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldPrimary
                        )
                        Text(
                            text = strings.aiSubtitle,
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Prompt Suggestion Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                strings.aiQuickPrompts.forEachIndexed { idx, prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(CharcoalElevated)
                            .border(1.dp, GoldBorder, RoundedCornerShape(50))
                            .clickable(enabled = !isBusy) {
                                onSendMessage(prompt)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("ai_quick_chip_$idx")
                    ) {
                        Text(
                            text = prompt,
                            style = MaterialTheme.typography.labelMedium,
                            color = GoldBright
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Chat Messages List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(chatMessages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }

                when (aiUiState) {
                    is AiUiState.Loading -> {
                        item(key = "ai_loading") {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(CharcoalElevated)
                                    .border(1.dp, GoldBorder, RoundedCornerShape(18.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = GoldPrimary
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = strings.aiLoadingText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    is AiUiState.Streaming -> {
                        item(key = "ai_streaming") {
                            ChatBubble(
                                message = ChatMessage(
                                    isUser = false,
                                    text = aiUiState.partialText
                                )
                            )
                        }
                    }

                    is AiUiState.Error -> {
                        item(key = "ai_error") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(CrimsonBg)
                                    .border(1.dp, CrimsonDown.copy(alpha = 0.6f), RoundedCornerShape(18.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ErrorOutline,
                                        contentDescription = null,
                                        tint = CrimsonDown,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = aiUiState.message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = TextIvory
                                    )
                                }
                                TextButton(onClick = onRetryLast) {
                                    Text(
                                        text = strings.aiRetry,
                                        color = GoldPrimary
                                    )
                                }
                            }
                        }
                    }

                    AiUiState.Idle -> Unit
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = {
                        Text(
                            text = strings.aiInputPlaceholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(22.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = GoldBorder,
                        focusedContainerColor = CharcoalCard,
                        unfocusedContainerColor = CharcoalCard,
                        focusedTextColor = TextIvory,
                        unfocusedTextColor = TextIvory,
                        cursorColor = GoldPrimary
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank() && !isBusy) {
                                onSendMessage(inputText)
                                inputText = ""
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input")
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank() && !isBusy) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                    enabled = inputText.isNotBlank() && !isBusy,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(50.dp)
                        .clip(CircleShape)
                        .background(
                            if (inputText.isNotBlank() && !isBusy) {
                                Brush.linearGradient(listOf(GoldBright, GoldDeep))
                            } else {
                                Brush.linearGradient(listOf(CharcoalElevated, CharcoalElevated))
                            }
                        )
                        .testTag("ai_send_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = strings.aiSendDesc,
                        tint = if (inputText.isNotBlank() && !isBusy) ObsidianBlack else TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 20.dp
                    )
                )
                .background(if (message.isUser) GoldSubtleBg else CharcoalElevated)
                .border(
                    width = 1.dp,
                    color = if (message.isUser) GoldPrimary.copy(alpha = 0.6f) else GoldBorder,
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 20.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = if (message.isUser) GoldBright else TextIvory
            )
        }
    }
}

@Composable
private fun SimulatedAdBoostDialog(
    remainingSeconds: Int,
    strings: AppStrings,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = CharcoalElevated,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = strings.aiAdTitle,
                style = MaterialTheme.typography.titleLarge,
                color = GoldBright
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(96.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { (30 - remainingSeconds) / 30f },
                        modifier = Modifier.fillMaxSize(),
                        color = GoldPrimary,
                        trackColor = GoldBorder,
                        strokeWidth = 6.dp
                    )
                    Text(
                        text = "${remainingSeconds}s",
                        style = MonoMetricMedium,
                        color = GoldBright
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = String.format(strings.aiAdCountdownLabel, remainingSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextIvory,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = strings.aiAdSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onCancel) {
                Text(text = strings.aiAdCancel, color = GoldPrimary)
            }
        }
    )
}
