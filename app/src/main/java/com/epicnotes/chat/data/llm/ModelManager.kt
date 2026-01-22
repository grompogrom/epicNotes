package com.epicnotes.chat.data.llm

import android.content.Context
import android.util.Log
import com.epicnotes.chat.data.llm.capability.DeviceCapabilityChecker
import com.epicnotes.chat.data.llm.error.DefaultLlmErrorHandler
import com.epicnotes.chat.data.llm.error.InsufficientDeviceCapabilityException
import com.epicnotes.chat.data.llm.error.LlmErrorHandler
import com.epicnotes.chat.data.llm.error.ModelInitializationException
import com.epicnotes.chat.data.llm.error.ModelNotInitializedException
import com.epicnotes.chat.data.llm.file.ModelFileManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Manages the lifecycle of the MediaPipe LLM model.
 * Handles model loading, initialization, and cleanup.
 *
 * This is a singleton to ensure only one model instance is loaded at a time.
 */
class ModelManager(
    private val context: Context,
    private val config: LlmConfig = LlmConfig(),
    private val metrics: LlmMetrics = LlmMetrics(context),
    private val fileManager: ModelFileManager,
    private val capabilityChecker: DeviceCapabilityChecker,
    private val errorHandler: LlmErrorHandler = DefaultLlmErrorHandler()
) {
    companion object {
        private const val TAG = "ModelManager"
        private const val INIT_TIMEOUT_MS = 60_000L
    }

    private var llmInference: LlmInference? = null
    private val mutex = Mutex()
    private var isInitialized = false
    private var initializationAttempts = 0

    /**
     * Initializes the LLM model with timeout protection.
     * This is a potentially long-running operation and should be called on a background thread.
     * Uses mutex to ensure thread-safe initialization.
     */
    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (isInitialized && llmInference != null) {
                Log.d(TAG, "Model already initialized")
                return@withContext
            }

            initializationAttempts++

            checkDeviceCapability()
            logMemoryState("Before model initialization")

            try {
                Log.i(TAG, "Initializing LLM model: ${config.modelPath} (attempt #$initializationAttempts)")
                val startTime = metrics.startModelLoad()

                fileManager.copyFromAssetsIfNeeded(config.modelPath)
                val modelPath = fileManager.getModelPath(config.modelPath)
                val modelFile = File(modelPath)

                fileManager.validateFile(modelFile)
                fileManager.verifyFileSize(modelFile, null)

                initializeInference(modelPath)

                isInitialized = true
                metrics.recordModelLoaded(startTime)
                logMemoryState("After successful model initialization")
                Log.i(TAG, "Model initialized successfully in ${System.currentTimeMillis() - startTime}ms")
            } catch (e: Exception) {
                handleInitializationError(e)
            }
        }
    }

    private fun checkDeviceCapability() {
        val deviceCheck = capabilityChecker.checkCapability()
        if (!deviceCheck.isCapable) {
            Log.w(TAG, "Device capability check failed: ${deviceCheck.warning}")
            throw InsufficientDeviceCapabilityException(
                deviceCheck.warning ?: "Device does not meet minimum requirements"
            )
        }

        if (deviceCheck.warning != null) {
            Log.w(TAG, "Device capability warning: ${deviceCheck.warning}")
        }

        if (capabilityChecker.isMemoryLow()) {
            Log.w(TAG, "⚠️ System reports low memory condition")
        }
    }

    private suspend fun initializeInference(modelPath: String) {
        withTimeout(INIT_TIMEOUT_MS) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(modelPath)
                .setMaxTokens(config.maxTokens)
                .setTemperature(config.temperature)
                .setTopK(config.topK)
                .setRandomSeed(config.randomSeed)
                .build()

            try {
                llmInference = LlmInference.createFromOptions(context, options)
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Native library error: ${e.message}", e)
                throw ModelInitializationException(
                    "MediaPipe native library error. The model may be incompatible with this MediaPipe version.",
                    e
                )
            } catch (e: Exception) {
                Log.e(TAG, "MediaPipe initialization error: ${e.javaClass.simpleName}: ${e.message}", e)
                throw e
            }
        }
    }

    private fun handleInitializationError(e: Exception) {
        Log.e(TAG, "Failed to initialize model: ${e.javaClass.simpleName}: ${e.message}", e)
        llmInference = null
        isInitialized = false
        metrics.recordModelLoadError(e)
        logMemoryState("After initialization failure")

        val exception = when (e) {
            is kotlinx.coroutines.TimeoutCancellationException -> {
                ModelInitializationException(
                    "Model initialization timed out. The model may be too large for this device.",
                    e
                )
            }
            is OutOfMemoryError -> {
                logMemoryState("After OOM error")
                ModelInitializationException(
                    "Out of memory while loading model. Please close other apps and try again.",
                    e
                )
            }
            is ModelInitializationException -> e
            else -> {
                val userMessage = errorHandler.handleInitializationException(
                    ModelInitializationException(e.message ?: "Unknown error", e)
                )
                ModelInitializationException(userMessage, e)
            }
        }

        throw exception
    }

    private fun logMemoryState(context: String) {
        if (config.enableMetrics) {
            metrics.logMemoryState(context)
        }
    }
    
    /**
     * Gets the initialized LLM inference instance.
     * Throws exception if model is not initialized.
     */
    suspend fun getInference(): LlmInference = mutex.withLock {
        llmInference ?: throw ModelNotInitializedException(
            "Model not initialized. Call initialize() first."
        )
    }
    
    /**
     * Checks if the model is initialized and ready for use.
     */
    suspend fun isReady(): Boolean = mutex.withLock {
        isInitialized && llmInference != null
    }
    
    /**
     * Releases the model resources.
     * Should be called when the model is no longer needed or to free memory.
     */
    suspend fun release() = withContext(Dispatchers.IO) {
        mutex.withLock {
            try {
                if (llmInference != null) {
                    metrics.logMemoryState("Before model release")
                    llmInference?.close()
                    llmInference = null
                    isInitialized = false
                    Log.i(TAG, "Model released successfully")

                    System.gc()
                    kotlinx.coroutines.delay(100)
                    metrics.logMemoryState("After model release")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error releasing model", e)
            }
        }
    }

    /**
     * Gets the metrics instance for monitoring.
     */
    fun getMetrics(): LlmMetrics = metrics
}
