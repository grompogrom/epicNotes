package com.epicnotes.chat.data.llm.error

/**
 * Exception thrown when model initialization fails.
 */
class ModelInitializationException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

/**
 * Exception thrown when trying to use an uninitialized model.
 */
class ModelNotInitializedException(message: String) : Exception(message)

/**
 * Exception thrown when device doesn't meet minimum requirements for LLM.
 */
class InsufficientDeviceCapabilityException(message: String) : Exception(message)

/**
 * Interface for handling LLM errors and converting them to user-friendly messages.
 * Centralizes error handling logic and provides consistent error messaging.
 */
interface LlmErrorHandler {

    /**
     * Converts a technical exception to a user-friendly IllegalStateException.
     * @param exception The technical exception
     * @return IllegalStateException with user-friendly message
     */
    fun handleException(exception: Exception): IllegalStateException

    /**
     * Converts model initialization exception to user-friendly message.
     * @param exception The ModelInitializationException
     * @return User-friendly error message
     */
    fun handleInitializationException(exception: ModelInitializationException): String

    /**
     * Converts insufficient device capability exception to user-friendly message.
     * @param exception The InsufficientDeviceCapabilityException
     * @return User-friendly error message
     */
    fun handleInsufficientCapabilityException(exception: InsufficientDeviceCapabilityException): String

    /**
     * Converts inference timeout to user-friendly message.
     * @param timeoutMs The timeout in milliseconds
     * @return User-friendly error message
     */
    fun handleTimeout(timeoutMs: Long): String

    /**
     * Converts out of memory error to user-friendly message.
     * @return User-friendly error message
     */
    fun handleOutOfMemory(): String
}
