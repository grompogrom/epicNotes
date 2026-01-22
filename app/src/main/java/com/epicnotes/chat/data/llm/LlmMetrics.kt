package com.epicnotes.chat.data.llm

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Metrics and logging for LLM operations.
 * Tracks performance, memory usage, and errors for debugging and monitoring.
 */
class LlmMetrics(private val context: Context) {
    
    companion object {
        private const val TAG = "LlmMetrics"
        private const val MB = 1024 * 1024
        private const val MEMORY_WARNING_THRESHOLD_MB = 100 // Warn if less than 100MB available
    }
    
    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    
    // Metrics state
    private val _metricsState = MutableStateFlow(MetricsState())
    val metricsState: StateFlow<MetricsState> = _metricsState.asStateFlow()
    
    /**
     * Records the start of model initialization.
     * @return Start time in milliseconds
     */
    fun startModelLoad(): Long {
        val startTime = System.currentTimeMillis()
        val memoryInfo = getMemoryInfo()
        
        Log.d(TAG, "Model load started - Available memory: ${memoryInfo.availableMemoryMB}MB, " +
                "Total: ${memoryInfo.totalMemoryMB}MB, " +
                "Used by app: ${memoryInfo.usedByAppMB}MB")
        
        return startTime
    }
    
    /**
     * Records successful model initialization.
     * @param startTime The time when loading started
     */
    fun recordModelLoaded(startTime: Long) {
        val loadTime = System.currentTimeMillis() - startTime
        val memoryInfo = getMemoryInfo()
        
        _metricsState.value = _metricsState.value.copy(
            totalModelLoads = _metricsState.value.totalModelLoads + 1,
            lastModelLoadTimeMs = loadTime,
            modelMemoryUsageMB = memoryInfo.usedByAppMB
        )
        
        Log.i(TAG, "Model loaded successfully in ${loadTime}ms - " +
                "Memory used by app: ${memoryInfo.usedByAppMB}MB")
    }
    
    /**
     * Records a model load failure.
     * @param error The error that occurred
     */
    fun recordModelLoadError(error: Throwable) {
        _metricsState.value = _metricsState.value.copy(
            totalErrors = _metricsState.value.totalErrors + 1,
            lastError = error.message ?: "Unknown error"
        )
        
        Log.e(TAG, "Model load failed: ${error.message}", error)
        logMemoryState("After model load failure")
    }
    
    /**
     * Records the start of inference.
     * @param promptLength Length of the prompt in characters
     * @return Start time in milliseconds
     */
    fun startInference(promptLength: Int): Long {
        val startTime = System.currentTimeMillis()
        val memoryInfo = getMemoryInfo()
        
        // Check memory pressure
        if (memoryInfo.availableMemoryMB < MEMORY_WARNING_THRESHOLD_MB) {
            Log.w(TAG, "⚠️ Low memory warning! Only ${memoryInfo.availableMemoryMB}MB available")
        }
        
        Log.d(TAG, "Inference started - Prompt length: $promptLength chars, " +
                "Available memory: ${memoryInfo.availableMemoryMB}MB")
        
        return startTime
    }
    
    /**
     * Records successful inference completion.
     * @param startTime The time when inference started
     * @param responseLength Length of the response in characters
     * @param tokensGenerated Number of tokens generated (approximate)
     */
    fun recordInference(startTime: Long, responseLength: Int, tokensGenerated: Int = responseLength / 4) {
        val inferenceTime = System.currentTimeMillis() - startTime
        val tokensPerSecond = if (inferenceTime > 0) {
            (tokensGenerated * 1000.0 / inferenceTime).toFloat()
        } else 0f
        
        val memoryInfo = getMemoryInfo()
        
        _metricsState.value = _metricsState.value.copy(
            totalInferences = _metricsState.value.totalInferences + 1,
            lastInferenceTimeMs = inferenceTime,
            averageInferenceTimeMs = calculateNewAverage(
                _metricsState.value.averageInferenceTimeMs,
                inferenceTime,
                _metricsState.value.totalInferences
            ),
            lastTokensPerSecond = tokensPerSecond,
            peakMemoryUsageMB = maxOf(_metricsState.value.peakMemoryUsageMB, memoryInfo.usedByAppMB)
        )
        
        Log.i(TAG, "✓ Inference completed in ${inferenceTime}ms - " +
                "Response: $responseLength chars, " +
                "Speed: ${"%.2f".format(tokensPerSecond)} tokens/sec, " +
                "Memory: ${memoryInfo.usedByAppMB}MB")
    }
    
    /**
     * Records an inference failure.
     * @param error The error that occurred
     * @param promptLength Length of the prompt that caused the error
     */
    fun recordInferenceError(error: Throwable, promptLength: Int) {
        _metricsState.value = _metricsState.value.copy(
            totalErrors = _metricsState.value.totalErrors + 1,
            lastError = error.message ?: "Unknown error"
        )
        
        Log.e(TAG, "Inference failed with prompt length $promptLength: ${error.message}", error)
        logMemoryState("After inference failure")
    }
    
    /**
     * Records inference cancellation by user.
     */
    fun recordInferenceCancelled() {
        Log.i(TAG, "Inference cancelled by user")
    }
    
    /**
     * Checks if device has sufficient memory for LLM operations.
     * @return true if device meets minimum requirements
     */
    fun checkDeviceCapability(): DeviceCheck {
        val memoryInfo = getMemoryInfo()
        val totalMemoryMB = memoryInfo.totalMemoryMB
        
        return when {
            totalMemoryMB < 3072 -> DeviceCheck(
                isCapable = false,
                warning = "Device has ${totalMemoryMB}MB RAM. Minimum 3GB recommended. App may crash or run slowly."
            )
            totalMemoryMB < 4096 -> DeviceCheck(
                isCapable = true,
                warning = "Device has ${totalMemoryMB}MB RAM. 4GB+ recommended for better performance."
            )
            else -> DeviceCheck(
                isCapable = true,
                warning = null
            )
        }
    }
    
    /**
     * Checks if system is under memory pressure.
     * @return true if memory is critically low
     */
    fun isMemoryLow(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }
    
    /**
     * Logs current memory state for debugging.
     */
    fun logMemoryState(context: String = "Current state") {
        val memoryInfo = getMemoryInfo()
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        Log.d(TAG, """
            |═══════════════════════════════════════
            |Memory State: $context
            |───────────────────────────────────────
            |Total Device Memory: ${memoryInfo.totalMemoryMB}MB
            |Available Memory: ${memoryInfo.availableMemoryMB}MB
            |Used by App: ${memoryInfo.usedByAppMB}MB
            |Native Heap: ${memoryInfo.nativeHeapMB}MB
            |Low Memory: ${memInfo.lowMemory}
            |Memory Threshold: ${memInfo.threshold / MB}MB
            |═══════════════════════════════════════
        """.trimMargin())
    }
    
    /**
     * Gets summary of performance metrics.
     */
    fun getPerformanceSummary(): String {
        val state = _metricsState.value
        return """
            |LLM Performance Metrics:
            |  Model Loads: ${state.totalModelLoads}
            |  Total Inferences: ${state.totalInferences}
            |  Avg Inference Time: ${state.averageInferenceTimeMs}ms
            |  Last Speed: ${"%.2f".format(state.lastTokensPerSecond)} tokens/sec
            |  Peak Memory: ${state.peakMemoryUsageMB}MB
            |  Total Errors: ${state.totalErrors}
        """.trimMargin()
    }
    
    /**
     * Resets all metrics (for testing or after major state changes).
     */
    fun reset() {
        _metricsState.value = MetricsState()
        Log.d(TAG, "Metrics reset")
    }
    
    // Private helper methods
    
    private fun getMemoryInfo(): MemoryInfo {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        
        val runtime = Runtime.getRuntime()
        val nativeHeapSize = Debug.getNativeHeapAllocatedSize()
        
        return MemoryInfo(
            totalMemoryMB = (memInfo.totalMem / MB).toInt(),
            availableMemoryMB = (memInfo.availMem / MB).toInt(),
            usedByAppMB = ((runtime.totalMemory() - runtime.freeMemory()) / MB).toInt(),
            nativeHeapMB = (nativeHeapSize / MB).toInt()
        )
    }
    
    private fun calculateNewAverage(oldAverage: Long, newValue: Long, count: Long): Long {
        if (count <= 1) return newValue
        return ((oldAverage * (count - 1)) + newValue) / count
    }
}

/**
 * Snapshot of current metrics state.
 */
data class MetricsState(
    val totalModelLoads: Long = 0,
    val totalInferences: Long = 0,
    val totalErrors: Long = 0,
    val lastModelLoadTimeMs: Long = 0,
    val lastInferenceTimeMs: Long = 0,
    val averageInferenceTimeMs: Long = 0,
    val lastTokensPerSecond: Float = 0f,
    val modelMemoryUsageMB: Int = 0,
    val peakMemoryUsageMB: Int = 0,
    val lastError: String? = null
)

/**
 * Memory information snapshot.
 */
private data class MemoryInfo(
    val totalMemoryMB: Int,
    val availableMemoryMB: Int,
    val usedByAppMB: Int,
    val nativeHeapMB: Int
)

/**
 * Result of device capability check.
 */
data class DeviceCheck(
    val isCapable: Boolean,
    val warning: String?
)
