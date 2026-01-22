package com.epicnotes.chat.presentation.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epicnotes.chat.domain.model.Sender
import com.epicnotes.chat.ui.theme.LoadingColor1
import com.epicnotes.chat.ui.theme.LoadingColor2
import com.epicnotes.chat.ui.theme.LoadingColor3
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.format.DateTimeFormatter

/**
 * Main Chat screen with sophisticated Telegram-inspired design.
 * Features mesmerizing loading animations and smooth transitions.
 */
@Composable
fun ChatScreen(
    viewModel: ChatViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Show error message as snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            coroutineScope.launch {
                snackbarHostState.showSnackbar(error)
                viewModel.onEvent(ChatEvent.ErrorDismissed)
            }
        }
    }

    // Auto-scroll to bottom when new messages arrive or during loading
    LaunchedEffect(uiState.messages.size, uiState.isSending) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list with custom spacing
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = paddingValues
            ) {
                if (uiState.messages.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Start a conversation",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        // Animate message appearance with slide-in and fade
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(
                                animationSpec = tween(durationMillis = 300)
                            ),
                            exit = fadeOut(
                                animationSpec = tween(durationMillis = 200)
                            )
                        ) {
                            MessageBubble(message = message)
                        }
                    }
                }

                // Animated loading bubble when LLM is processing
                // Shows mesmerizing color gradient + animated dots
                // Will smoothly transition out when response arrives
                if (uiState.isSending) {
                    item(key = "loading") {
                        AnimatedVisibility(
                            visible = true,
                            enter = expandVertically(
                                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                            ) + fadeIn(
                                animationSpec = tween(durationMillis = 400)
                            ),
                            exit = shrinkVertically(
                                animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)
                            ) + fadeOut(
                                animationSpec = tween(durationMillis = 300)
                            )
                        ) {
                            LoadingBubble()
                        }
                    }
                }
            }

            // Input field with refined styling
            InputBar(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                onInputChanged = { text ->
                    viewModel.onEvent(ChatEvent.InputChanged(text))
                },
                onSendClicked = {
                    viewModel.onEvent(ChatEvent.SendClicked)
                },
                onCancelClicked = {
                    viewModel.onEvent(ChatEvent.CancelClicked)
                }
            )
        }
    }
}

/**
 * Sophisticated animated loading bubble with mesmerizing color-shifting effect.
 * Creates a smooth gradient cycle (blue → purple → pink) with pulsating animation.
 */
@Composable
private fun LoadingBubble() {
    val infiniteTransition = rememberInfiniteTransition(label = "loadingAnimation")

    // Animate gradient colors through a cycle
    val animatedProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradientProgress"
    )

    // Pulsating scale animation
    val animatedScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulsateScale"
    )

    // Animated dots with wave effect
    val dotPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dotPhase"
    )

    // Create dynamic gradient based on progress
    val brush = Brush.horizontalGradient(
        colorStops = arrayOf(
            0.0f to LoadingColor1,
            0.5f to LoadingColor2,
            1.0f to LoadingColor3,
            (1.0f + animatedProgress) % 2f to LoadingColor1,
            (1.5f + animatedProgress) % 2f to LoadingColor2
        )
    )

    // Same corner radius as assistant messages
    val cornerRadius = RoundedCornerShape(
        topStart = 4.dp,
        topEnd = 16.dp,
        bottomStart = 16.dp,
        bottomEnd = 16.dp
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(cornerRadius)
                .background(brush)
                .padding(16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated dots with wave effect
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.alpha(animatedScale)
                ) {
                    repeat(3) { index ->
                        val dotAlpha = remember {
                            Animatable(0.3f)
                        }
                        LaunchedEffect(dotPhase) {
                            val phase = (dotPhase + index * 0.33f) % 1f
                            dotAlpha.animateTo(
                                if (phase > 0.5f) 0.3f else 0.9f,
                                animationSpec = tween(300, easing = LinearEasing)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    Color.White,
                                    RoundedCornerShape(50)
                                )
                                .alpha(dotAlpha.value)
                        )
                    }
                }

                // Animated text
                Text(
                    text = "AI is thinking",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.alpha(0.9f)
                )
            }
        }
    }
}

/**
 * Telegram-style message bubble with asymmetric corner radius.
 * User messages aligned right, assistant messages aligned left.
 * Features smooth animation when content changes.
 */
@Composable
private fun MessageBubble(message: com.epicnotes.chat.domain.model.ChatMessage) {
    val isUser = message.sender == Sender.USER
    val backgroundColor by animateColorAsState(
        targetValue = if (isUser) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "backgroundColor"
    )

    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }

    // Telegram-style asymmetric corner radius
    val cornerRadius = if (isUser) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 4.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp),
        contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(cornerRadius)
                .background(backgroundColor)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Column {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (isUser) FontWeight.Normal else FontWeight.Normal
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = message.timestamp.format(
                            DateTimeFormatter.ofPattern("HH:mm")
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * Refined input bar with modern styling.
 * Features clear send/cancel states and clean typography.
 */
@Composable
private fun InputBar(
    inputText: String,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onCancelClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Modern text field
        TextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text(
                    "Type a message...",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            },
            enabled = !isSending,
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = {
                    if (inputText.isNotBlank()) {
                        onSendClicked()
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(24.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // Send or Cancel button
        IconButton(
            onClick = if (isSending) onCancelClicked else onSendClicked,
            enabled = inputText.isNotBlank() || isSending,
            modifier = Modifier.size(48.dp)
        ) {
            if (isSending) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = if (inputText.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    }
                )
            }
        }
    }
}
