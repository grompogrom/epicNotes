package com.epicnotes.chat.presentation.chat

import com.epicnotes.chat.domain.model.ChatMessage

/**
 * Immutable UI state for the Chat screen.
 * Follows unidirectional data flow principles.
 *
 * @property messages List of all messages in the conversation
 * @property inputText Current text in the input field
 * @property isSending Whether a message is currently being sent
 * @property error Error message if sending failed, null otherwise
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null
)
