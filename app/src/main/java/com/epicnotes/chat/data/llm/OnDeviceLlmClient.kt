package com.epicnotes.chat.data.llm

import android.util.Log
import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.model.ChatMessage
import com.epicnotes.chat.domain.model.Sender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.currentCoroutineContext

/**
 * On-device LLM implementation using MediaPipe with comprehensive error handling.
 * Implements the LlmClient interface for Gemma 2B model inference.
 * 
 * This implementation runs the LLM completely on-device without any network calls.
 * Includes metrics tracking, memory monitoring, and cancellation support.
 */
class OnDeviceLlmClient(
    private val modelManager: ModelManager,
    private val config: LlmConfig = LlmConfig()
) : LlmClient {
    
    companion object {
        private const val TAG = "OnDeviceLlmClient"
        
        // Gemma 2 chat template tokens
        private const val START_OF_TURN = "<start_of_turn>"
        private const val END_OF_TURN = "<end_of_turn>"
        private const val USER_ROLE = "user"
        private const val MODEL_ROLE = "model"
        
        // Limits
        private const val MAX_PROMPT_LENGTH = 8000 // characters
    }
    
    private val metrics = modelManager.getMetrics()
    
    /**
     * Generates a response based on the conversation history with comprehensive error handling.
     * Formats messages using Gemma's chat template and performs on-device inference.
     * 
     * Supports cancellation and includes metrics tracking.
     * 
     * @param messages List of previous messages in the conversation
     * @return The assistant's response as a string
     * @throws IllegalStateException if model initialization fails
     * @throws OutOfMemoryError if system runs out of memory
     * @throws kotlinx.coroutines.CancellationException if operation is cancelled
     */
    override suspend fun replyTo(messages: List<ChatMessage>): String = withContext(Dispatchers.IO) {
        val prompt = try {
            // Check for cancellation
            currentCoroutineContext().ensureActive()
            
            // Check memory before starting
            if (metrics.isMemoryLow()) {
                Log.w(TAG, "⚠️ Low memory detected before inference")
                metrics.logMemoryState("Before inference (low memory)")
            }
            
            // Ensure model is initialized
            if (!modelManager.isReady()) {
                Log.i(TAG, "Model not ready, initializing...")
                try {
                    modelManager.initialize()
                } catch (e: InsufficientDeviceCapabilityException) {
                    throw IllegalStateException(
                        "Your device does not meet the minimum requirements to run the AI model: ${e.message}",
                        e
                    )
                } catch (e: ModelInitializationException) {
                    throw IllegalStateException(
                        "Failed to initialize AI model: ${e.message}",
                        e
                    )
                }
            }
            
            // Format messages using Gemma chat template
            val formattedPrompt = formatPrompt(messages)
            
            // Validate prompt length
            if (formattedPrompt.length > MAX_PROMPT_LENGTH) {
                Log.w(TAG, "Prompt too long (${formattedPrompt.length} chars), truncating...")
                // Keep only recent messages to fit within limit
                val recentMessages = messages.takeLast(3)
                formatPrompt(recentMessages)
            } else {
                formattedPrompt
            }
        } catch (e: Exception) {
            if (e is IllegalStateException) throw e
            Log.e(TAG, "Error preparing inference", e)
            throw IllegalStateException("Failed to prepare AI inference: ${e.message}", e)
        }
        
        Log.d(TAG, "Generated prompt (${prompt.length} chars)")
        
        try {
            // Check for cancellation again before expensive operation
            currentCoroutineContext().ensureActive()
            
            // Start metrics tracking
            val startTime = metrics.startInference(prompt.length)
            
            // Get inference instance
            val inference = modelManager.getInference()
            
            // Perform inference with timeout
            val response = try {
                withTimeout(config.inferenceTimeoutMs) {
                    inference.generateResponse(prompt)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                metrics.recordInferenceError(e, prompt.length)
                throw IllegalStateException(
                    "AI response timed out after ${config.inferenceTimeoutMs / 1000} seconds. " +
                    "Try asking a simpler question.",
                    e
                )
            }
            
            // Record successful inference
            metrics.recordInference(startTime, response.length)
            
            // Clean up the response
            val cleanedResponse = cleanResponse(response)
            
            if (cleanedResponse.isBlank()) {
                Log.w(TAG, "Generated response is blank")
                return@withContext "I apologize, but I couldn't generate a proper response. Please try rephrasing your question."
            }
            
            return@withContext cleanedResponse
            
        } catch (e: kotlinx.coroutines.CancellationException) {
            // User cancelled - this is normal, just propagate it
            Log.i(TAG, "Inference cancelled by user")
            metrics.recordInferenceCancelled()
            throw e
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "Out of memory during inference", e)
            metrics.recordInferenceError(e, prompt.length)
            metrics.logMemoryState("After OOM during inference")
            
            // Try to release model to free memory
            try {
                modelManager.release()
            } catch (releaseError: Exception) {
                Log.e(TAG, "Failed to release model after OOM", releaseError)
            }
            
            throw IllegalStateException(
                "Out of memory while generating response. Please close other apps and try again.",
                e
            )
        } catch (e: ModelNotInitializedException) {
            Log.e(TAG, "Model not initialized during inference", e)
            metrics.recordInferenceError(e, prompt.length)
            throw IllegalStateException("AI model not initialized", e)
        } catch (e: IllegalStateException) {
            // Already formatted error message, just rethrow
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference", e)
            metrics.recordInferenceError(e, prompt.length)
            throw IllegalStateException(
                "Failed to generate AI response: ${e.message ?: "Unknown error"}",
                e
            )
        }
    }
    
    /**
     * Formats the conversation history using Gemma's chat template.
     * 
     * Format: <start_of_turn>user\n{message}<end_of_turn>\n<start_of_turn>model\n{response}<end_of_turn>\n
     */
    private fun formatPrompt(messages: List<ChatMessage>): String {
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
        
        // Add the model turn marker to prompt for generation
        builder.append(START_OF_TURN)
        builder.append(MODEL_ROLE)
        builder.append("\n")
        
        return builder.toString()
    }
    
    /**
     * Cleans the generated response by removing special tokens and extra whitespace.
     */
    private fun cleanResponse(response: String): String {
        return response
            .replace(START_OF_TURN, "")
            .replace(END_OF_TURN, "")
            .replace(MODEL_ROLE, "")
            .replace(USER_ROLE, "")
            .trim()
    }
}
