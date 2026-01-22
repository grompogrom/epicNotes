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
 * @property progressMessage Optional message to show during long-running operations
 * @property estimatedTimeSeconds Estimated time remaining for current operation (0 if unknown)
 * @property canCancel Whether the current operation can be cancelled
 */
data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isSending: Boolean = false,
    val error: String? = null,
    val progressMessage: String? = null,
    val estimatedTimeSeconds: Int = 0,
    val canCancel: Boolean = false
)
