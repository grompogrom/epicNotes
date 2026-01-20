package com.epicnotes.chat.domain.model

/**
 * Enum representing the sender of a chat message.
 * Following Clean Architecture - domain layer only, no Android dependencies.
 */
enum class Sender {
    USER,
    ASSISTANT
}
