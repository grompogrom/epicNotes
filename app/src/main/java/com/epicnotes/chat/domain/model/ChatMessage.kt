package com.epicnotes.chat.domain.model

import java.time.LocalDateTime
import java.util.UUID

/**
 * Represents a single message in the chat conversation.
 * Immutable data class following Clean Architecture principles.
 *
 * @property id Unique identifier for the message
 * @property content The actual message text
 * @property sender Who sent the message (USER or ASSISTANT)
 * @property timestamp When the message was sent
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val content: String,
    val sender: Sender,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
