package com.epicnotes.chat.data.llm

import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.model.ChatMessage
import kotlinx.coroutines.delay

/**
 * Mock implementation of LlmClient for testing and development.
 * Simulates network delay and returns a fixed response.
 *
 * This implementation demonstrates Clean Architecture by implementing
 * the domain interface in the data layer without any Android dependencies.
 */
class MockLlmClient : LlmClient {
    override suspend fun replyTo(messages: List<ChatMessage>): String {
        // Simulate network/API delay
        delay(300)

        // Return a fixed response for demonstration
        return "I dont know"
    }
}
