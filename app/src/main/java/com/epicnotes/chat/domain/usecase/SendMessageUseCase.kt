package com.epicnotes.chat.domain.usecase

import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.model.ChatMessage
import com.epicnotes.chat.domain.model.Sender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Use case for sending messages and getting AI responses.
 * Coordinates the flow of adding a user message and getting an assistant response.
 *
 * This use case is pure Kotlin with no Android dependencies, following Clean Architecture.
 */
class SendMessageUseCase(
    private val llmClient: LlmClient
) {
    /**
     * Executes the message sending flow:
     * 1. Returns the user message to be added to the UI
     * 2. Calls LLM to get assistant response
     * 3. Returns the assistant message
     *
     * @param userContent The text content of the user's message
     * @param conversationHistory List of previous messages for context
     * @return Pair containing (userMessage, assistantMessage) where assistantMessage can be null on error
     */
    suspend operator fun invoke(
        userContent: String,
        conversationHistory: List<ChatMessage>
    ): Result<Pair<ChatMessage, ChatMessage?>> = withContext(Dispatchers.IO) {
        try {
            // Create user message
            val userMessage = ChatMessage(
                content = userContent,
                sender = Sender.USER
            )

            // Get assistant response from LLM
            val response = llmClient.replyTo(conversationHistory + userMessage)

            // Create assistant message
            val assistantMessage = ChatMessage(
                content = response,
                sender = Sender.ASSISTANT
            )

            Result.success(Pair(userMessage, assistantMessage))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
