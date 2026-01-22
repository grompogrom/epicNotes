package com.epicnotes.chat.data.llm.formatting

import com.epicnotes.chat.domain.model.ChatMessage
import com.epicnotes.chat.domain.model.Sender

/**
 * Gemma-specific prompt formatter using Gemma 2 chat template.
 * Format: <start_of_turn>user\n{message}<end_of_turn>\n<start_of_turn>model\n{response}<end_of_turn>\n
 */
class GemmaPromptFormatter : PromptFormatter {

    companion object {
        private const val START_OF_TURN = "<start_of_turn>"
        private const val END_OF_TURN = "<end_of_turn>"
        private const val USER_ROLE = "user"
        private const val MODEL_ROLE = "model"
    }

    override fun format(messages: List<ChatMessage>): String {
        val builder = StringBuilder()

        for (message in messages) {
            val role = when (message.sender) {
                Sender.USER -> USER_ROLE
                Sender.ASSISTANT -> MODEL_ROLE
            }

            builder.append(START_OF_TURN)
            builder.append(role)
            builder.append("\n")
            builder.append(message.content)
            builder.append(END_OF_TURN)
            builder.append("\n")
        }

        builder.append(START_OF_TURN)
        builder.append(MODEL_ROLE)
        builder.append("\n")

        return builder.toString()
    }

    override fun truncateToFit(messages: List<ChatMessage>, maxLength: Int): List<ChatMessage> {
        if (isValidLength(format(messages), maxLength)) {
            return messages
        }

        var remainingMessages = messages
        var messageCount = messages.size

        while (messageCount > 1) {
            messageCount = (messageCount * 2) / 3
            remainingMessages = messages.takeLast(messageCount)

            if (isValidLength(format(remainingMessages), maxLength)) {
                return remainingMessages
            }
        }

        return remainingMessages
    }
}
