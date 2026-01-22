package com.epicnotes.chat.data.llm.formatting

import com.epicnotes.chat.domain.model.ChatMessage

/**
 * Interface for formatting conversation history into LLM prompts.
 * Allows different prompt templates for different models.
 */
interface PromptFormatter {

    /**
     * Formats the conversation history into a prompt string.
     * @param messages List of previous messages in the conversation
     * @return Formatted prompt string
     */
    fun format(messages: List<ChatMessage>): String

    /**
     * Validates the prompt length against a maximum limit.
     * @param prompt The prompt to validate
     * @param maxLength Maximum allowed length in characters
     * @return true if prompt is within limits, false otherwise
     */
    fun isValidLength(prompt: String, maxLength: Int): Boolean = prompt.length <= maxLength

    /**
     * Truncates the conversation to fit within the maximum prompt length.
     * Keeps the most recent messages.
     * @param messages List of messages to truncate
     * @param maxLength Maximum allowed prompt length in characters
     * @return Truncated list of messages that fits within the limit
     */
    fun truncateToFit(messages: List<ChatMessage>, maxLength: Int): List<ChatMessage>
}
