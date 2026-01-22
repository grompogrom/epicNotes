package com.epicnotes.chat.data.llm.error

/**
 * Default implementation of LlmErrorHandler.
 * Provides user-friendly error messages for common LLM errors.
 */
class DefaultLlmErrorHandler : LlmErrorHandler {

    override fun handleException(exception: Exception): IllegalStateException {
        return when (exception) {
            is ModelInitializationException ->
                IllegalStateException(handleInitializationException(exception), exception)
            is InsufficientDeviceCapabilityException ->
                IllegalStateException(handleInsufficientCapabilityException(exception), exception)
            is OutOfMemoryError ->
                IllegalStateException(handleOutOfMemory(), exception)
            is ModelNotInitializedException ->
                IllegalStateException("AI model not initialized", exception)
            else ->
                IllegalStateException(
                    "Failed to generate AI response: ${exception.message ?: "Unknown error"}",
                    exception
                )
        }
    }

    override fun handleInitializationException(exception: ModelInitializationException): String {
        val errorMsg = exception.message ?: "Unknown error"
        return when {
            errorMsg.contains("zip", ignoreCase = true) || errorMsg.contains("archive", ignoreCase = true) ->
                "Model file appears to be corrupted or incomplete. Please re-download the model file."
            else ->
                "Failed to initialize AI model: $errorMsg"
        }
    }

    override fun handleInsufficientCapabilityException(exception: InsufficientDeviceCapabilityException): String {
        return "Your device does not meet the minimum requirements to run the AI model: ${exception.message}"
    }

    override fun handleTimeout(timeoutMs: Long): String {
        val seconds = timeoutMs / 1000
        return "AI response timed out after $seconds seconds. Try asking a simpler question."
    }

    override fun handleOutOfMemory(): String {
        return "Out of memory while generating response. Please close other apps and try again."
    }
}
