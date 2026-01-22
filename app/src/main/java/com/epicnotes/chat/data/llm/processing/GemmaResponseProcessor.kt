package com.epicnotes.chat.data.llm.processing

/**
 * Gemma-specific response processor.
 * Removes special tokens and normalizes whitespace.
 */
class GemmaResponseProcessor : ResponseProcessor {

    companion object {
        private const val START_OF_TURN = "<start_of_turn>"
        private const val END_OF_TURN = "<end_of_turn>"
        private const val MODEL_ROLE = "model"
        private const val USER_ROLE = "user"
    }

    override fun clean(response: String): String {
        return response
            .replace(START_OF_TURN, "")
            .replace(END_OF_TURN, "")
            .replace(MODEL_ROLE, "")
            .replace(USER_ROLE, "")
            .trim()
    }
}
