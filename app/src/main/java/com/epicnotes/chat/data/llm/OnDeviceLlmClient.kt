package com.epicnotes.chat.data.llm

import android.util.Log
import com.epicnotes.chat.data.llm.error.LlmErrorHandler
import com.epicnotes.chat.data.llm.formatting.PromptFormatter
import com.epicnotes.chat.data.llm.processing.ResponseProcessor
import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * On-device LLM implementation using MediaPipe.
 * Implements the LlmClient interface for Gemma 2B model inference.
 *
 * This implementation runs the LLM completely on-device without any network calls.
 * Includes metrics tracking, memory monitoring, and cancellation support.
 */
class OnDeviceLlmClient(
    private val modelManager: ModelManager,
    private val config: LlmConfig = LlmConfig(),
    private val promptFormatter: PromptFormatter,
    private val responseProcessor: ResponseProcessor,
    private val errorHandler: LlmErrorHandler
) : LlmClient {

    companion object {
        private const val TAG = "OnDeviceLlmClient"
        private const val MAX_PROMPT_LENGTH = 8000
    }

    private val metrics = modelManager.getMetrics()
    
    /**
     * Generates a response based on the conversation history.
     * Formats messages and performs on-device inference.
     *
     * Supports cancellation and includes metrics tracking.
     *
     * @param messages List of previous messages in the conversation
     * @return The assistant's response as a string
     */
    override suspend fun replyTo(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val prompt = preparePrompt(messages)
        Log.d(TAG, "Generated prompt (${prompt.length} chars)")

        performInference(prompt)
    }

    private suspend fun preparePrompt(messages: List<ChatMessage>): String {
        currentCoroutineContext().ensureActive()

        checkMemoryBeforeInference()
        ensureModelInitialized()

        val formattedPrompt = promptFormatter.format(messages)
        validatePromptLength(formattedPrompt, messages)

        return formattedPrompt
    }

    private fun checkMemoryBeforeInference() {
        if (metrics.isMemoryLow()) {
            Log.w(TAG, "⚠️ Low memory detected before inference")
            metrics.logMemoryState("Before inference (low memory)")
        }
    }

    private suspend fun ensureModelInitialized() {
        if (!modelManager.isReady()) {
            Log.i(TAG, "Model not ready, initializing...")
            modelManager.initialize()
        }
    }

    private fun validatePromptLength(prompt: String, messages: List<ChatMessage>): String {
        return if (prompt.length > MAX_PROMPT_LENGTH) {
            Log.w(TAG, "Prompt too long (${prompt.length} chars), truncating...")
            val truncatedMessages = promptFormatter.truncateToFit(messages, MAX_PROMPT_LENGTH)
            promptFormatter.format(truncatedMessages)
        } else {
            prompt
        }
    }

    private suspend fun performInference(prompt: String): String {
        currentCoroutineContext().ensureActive()

        val startTime = metrics.startInference(prompt.length)
        val inference = modelManager.getInference()

        val response = try {
            withTimeout(config.inferenceTimeoutMs) {
                inference.generateResponse(prompt)
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            Log.i(TAG, "Inference cancelled by user")
            metrics.recordInferenceCancelled()
            throw e
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            metrics.recordInferenceError(e, prompt.length)
            throw IllegalStateException(errorHandler.handleTimeout(config.inferenceTimeoutMs), e)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during inference", e)
            metrics.recordInferenceError(e, prompt.length)
            metrics.logMemoryState("After OOM during inference")

            try {
                modelManager.release()
            } catch (releaseError: Exception) {
                Log.e(TAG, "Failed to release model after OOM", releaseError)
            }

            throw IllegalStateException(errorHandler.handleOutOfMemory(), e)
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference", e)
            metrics.recordInferenceError(e, prompt.length)
            throw errorHandler.handleException(e as Exception)
        }

        metrics.recordInference(startTime, response.length)
        val cleanedResponse = responseProcessor.clean(response)

        return if (cleanedResponse.isBlank()) {
            Log.w(TAG, "Generated response is blank")
            "I apologize, but I couldn't generate a proper response. Please try rephrasing your question."
        } else {
            cleanedResponse
        }
    }
}
