package com.epicnotes.chat.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.epicnotes.chat.domain.model.Sender
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.time.format.DateTimeFormatter

/**
 * Main Chat screen composable.
 * Displays a list of messages with an input field at the bottom.
 *
 * Uses Material 3 design principles with proper state management.
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

    // Auto-scroll to bottom when new messages arrive
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Messages list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        MessageBubble(message = message)
                    }
                }
            }

            // Input field
            InputBar(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                onInputChanged = { text ->
                    viewModel.onEvent(ChatEvent.InputChanged(text))
                },
                onSendClicked = {
                    viewModel.onEvent(ChatEvent.SendClicked)
                }
            )
        }
    }
}

/**
 * Composable for a single message bubble.
 * Different styling for user vs assistant messages.
 */
@Composable
private fun MessageBubble(message: com.epicnotes.chat.domain.model.ChatMessage) {
    val isUser = message.sender == Sender.USER
    val backgroundColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    val alignment = if (isUser) {
        Alignment.CenterEnd
    } else {
        Alignment.CenterStart
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        contentAlignment = alignment
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = if (isUser) FontWeight.Bold else FontWeight.Normal
                )
                Text(
                    text = message.timestamp.format(
                        DateTimeFormatter.ofPattern("HH:mm")
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

/**
 * Input bar with text field and send button.
 */
@Composable
private fun InputBar(
    inputText: String,
    isSending: Boolean,
    onInputChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputChanged,
            modifier = Modifier.weight(1f),
            placeholder = {
                Text("Type a message...")
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
            shape = RoundedCornerShape(24.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSendClicked,
            enabled = inputText.isNotBlank() && !isSending,
            modifier = Modifier.padding(start = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            if (isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(8.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}
