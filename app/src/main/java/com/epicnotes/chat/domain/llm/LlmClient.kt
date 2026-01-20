package com.epicnotes.chat.domain.llm

import com.epicnotes.chat.domain.model.ChatMessage

/**
 * Interface for LLM (Large Language Model) client communication.
 * Defines the contract for AI-powered chat responses.
 *
 * Implementations can be real API clients, mock clients, or any other LLM integration.
 * This interface is platform-independent with no Android dependencies.
 */
interface LlmClient {
    /**
     * Generates a response based on the conversation history.
     *
     * @param messages List of previous messages in the conversation
     * @return The assistant's response as a string
     */
    suspend fun replyTo(messages: List<ChatMessage>): String
}
