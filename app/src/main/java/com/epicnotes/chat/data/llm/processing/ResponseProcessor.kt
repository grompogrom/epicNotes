package com.epicnotes.chat.data.llm.processing

/**
 * Interface for processing and cleaning LLM responses.
 * Handles token removal and text normalization.
 */
interface ResponseProcessor {

    /**
     * Cleans the generated response by removing special tokens and normalizing text.
     * @param response Raw response from the model
     * @return Cleaned response string
     */
    fun clean(response: String): String

    /**
     * Checks if the cleaned response is blank or empty.
     * @param response The response to validate
     * @return true if response is blank, false otherwise
     */
    fun isBlank(response: String): Boolean = clean(response).isBlank()
}
