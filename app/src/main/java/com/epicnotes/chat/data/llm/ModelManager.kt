package com.epicnotes.chat.data.llm

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/**
 * Manages the lifecycle of the MediaPipe LLM model.
 * Handles model loading, initialization, and cleanup with memory monitoring.
 * 
 * This is a singleton to ensure only one model instance is loaded at a time.
 */
class ModelManager(
    private val context: Context,
    private val config: LlmConfig = LlmConfig(),
    private val metrics: LlmMetrics = LlmMetrics(context)
) {
    companion object {
        private const val TAG = "ModelManager"
        private const val INIT_TIMEOUT_MS = 60_000L // 60 seconds timeout for initialization
    }
    
    private var llmInference: LlmInference? = null
    private val mutex = Mutex()
    private var isInitialized = false
    private var initializationAttempts = 0
    
    /**
     * Initializes the LLM model with memory monitoring and timeout protection.
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
            
            // Check device capability before attempting initialization
            val deviceCheck = metrics.checkDeviceCapability()
            if (!deviceCheck.isCapable) {
                Log.w(TAG, "Device capability check failed: ${deviceCheck.warning}")
                throw InsufficientDeviceCapabilityException(
                    deviceCheck.warning ?: "Device does not meet minimum requirements"
                )
            }
            
            if (deviceCheck.warning != null) {
                Log.w(TAG, "Device capability warning: ${deviceCheck.warning}")
            }
            
            // Check memory state before loading
            metrics.logMemoryState("Before model initialization")
            if (metrics.isMemoryLow()) {
                Log.w(TAG, "⚠️ System reports low memory condition")
            }
            
                // Model file location - check internal storage first (for manually pushed models)
                // Large models (>2GB) are excluded from APK to avoid ZIP32 4GB limit
                val modelFileName = config.modelPath.substringAfterLast("/")
                val modelsDir = java.io.File(context.filesDir, "models")
                modelsDir.mkdirs()
                val internalModelFile = java.io.File(modelsDir, modelFileName)
                val modelPathForMediaPipe: String
            
            try {
                Log.i(TAG, "Initializing LLM model: ${config.modelPath} (attempt #$initializationAttempts)")
                
                val startTime = metrics.startModelLoad()
                
                // Track if file was copied from assets (for size verification)
                var fileCopiedFromAssets = false
                var expectedAssetSize: Long? = null
                
                // Check if model exists in internal storage (for manually pushed models)
                if (!internalModelFile.exists() || internalModelFile.length() == 0L) {
                    // Try to copy from assets (if model is small enough to be in APK)
                    try {
                        Log.i(TAG, "Model not found in internal storage, checking assets: ${config.modelPath}")
                        context.assets.open(config.modelPath).use { input ->
                            expectedAssetSize = input.available().toLong()
                            Log.i(TAG, "Found model in assets (${expectedAssetSize!! / (1024 * 1024)}MB), copying to internal storage...")
                            
                            internalModelFile.outputStream().use { output ->
                                var bytesCopied = 0L
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    bytesCopied += bytesRead
                                }
                            }
                        }
                        fileCopiedFromAssets = true
                        Log.i(TAG, "Model copied successfully (${internalModelFile.length() / (1024 * 1024)}MB)")
                    } catch (e: java.io.FileNotFoundException) {
                        // Model not in assets (excluded from APK due to size)
                        val errorMsg = """
                            Model file not found. Large models (>2GB) are excluded from APK.
                            
                            Please push the model file manually via ADB:
                            adb push app/src/main/assets/models/$modelFileName /data/local/tmp/
                            adb shell run-as com.epicnotes.chat cp /data/local/tmp/$modelFileName files/models/
                            
                            Or use the provided script: ./scripts/push_model.sh
                        """.trimIndent()
                        Log.e(TAG, errorMsg)
                        throw ModelInitializationException(
                            "Model file not found. Please push the model file to the device manually. " +
                            "See logs for instructions.",
                            e
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to copy model: ${e.message}", e)
                        throw ModelInitializationException("Failed to copy model: ${e.message}", e)
                    }
                } else {
                    Log.d(TAG, "Model found in internal storage (${internalModelFile.length() / (1024 * 1024)}MB)")
                }
                
                // Verify file exists and has content
                if (!internalModelFile.exists() || internalModelFile.length() == 0L) {
                    throw ModelInitializationException("Model file not found or empty after copy: ${internalModelFile.absolutePath}")
                }
                
                // Verify file is readable
                try {
                    internalModelFile.inputStream().use { input ->
                        val header = ByteArray(4)
                        if (input.read(header) < 4) {
                            throw ModelInitializationException("Model file is too small or corrupted")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read model file: ${e.message}", e)
                    throw ModelInitializationException("Model file is not readable: ${e.message}. File may be corrupted.", e)
                }
                
                modelPathForMediaPipe = internalModelFile.absolutePath
                
                // Use timeout to prevent indefinite hanging
                withTimeout(INIT_TIMEOUT_MS) {
                    // Verify file is complete (only if copied from assets)
                    val actualSize = internalModelFile.length()
                    if (fileCopiedFromAssets && expectedAssetSize != null) {
                        if (actualSize != expectedAssetSize) {
                            throw ModelInitializationException(
                                "Model file size mismatch: expected ${expectedAssetSize}bytes, got ${actualSize}bytes. File may be corrupted."
                            )
                        }
                    } else {
                        // File was manually pushed - just verify it's not empty
                        if (actualSize == 0L) {
                            throw ModelInitializationException("Model file is empty (0 bytes). File may be corrupted.")
                        }
                    }
                    
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelPathForMediaPipe)
                        .setMaxTokens(config.maxTokens)
                        .setTemperature(config.temperature)
                        .setTopK(config.topK)
                        .setRandomSeed(config.randomSeed)
                        .build()
                    
                    try {
                        llmInference = LlmInference.createFromOptions(context, options)
                        Log.i(TAG, "Model initialized successfully in ${System.currentTimeMillis() - startTime}ms")
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
                
                isInitialized = true
                metrics.recordModelLoaded(startTime)
                metrics.logMemoryState("After successful model initialization")
                
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.e(TAG, "Model initialization timed out after ${INIT_TIMEOUT_MS}ms", e)
                llmInference = null
                isInitialized = false
                metrics.recordModelLoadError(e)
                throw ModelInitializationException(
                    "Model initialization timed out. The model may be too large for this device.",
                    e
                )
            } catch (e: OutOfMemoryError) {
                Log.e(TAG, "Out of memory during model initialization", e)
                llmInference = null
                isInitialized = false
                metrics.recordModelLoadError(e)
                metrics.logMemoryState("After OOM error")
                throw ModelInitializationException(
                    "Out of memory while loading model. Please close other apps and try again.",
                    e
                )
            } catch (e: Exception) {
                val errorMsg = e.message ?: "null"
                Log.e(TAG, "Failed to initialize model: ${e.javaClass.simpleName}: $errorMsg", e)
                llmInference = null
                isInitialized = false
                metrics.recordModelLoadError(e)
                metrics.logMemoryState("After initialization failure")
                
                // Provide user-friendly error message
                val userMessage = when {
                    errorMsg.contains("zip", ignoreCase = true) || errorMsg.contains("archive", ignoreCase = true) ->
                        "Model file appears to be corrupted or incomplete. Please re-download the model file."
                    else ->
                        "Failed to load LLM model: $errorMsg"
                }
                
                throw ModelInitializationException(userMessage, e)
            }
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
                    
                    // Give system time to reclaim memory
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
