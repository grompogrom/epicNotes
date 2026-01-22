package com.epicnotes.chat.data.llm

/**
 * Configuration for on-device LLM inference.
 * Contains model-specific parameters, inference settings, and safety limits.
 */
data class LlmConfig(
    /** 
     * Path to the model file in assets.
     * IMPORTANT: MediaPipe requires .task format files, NOT .bin files!
     * 
     * Download MediaPipe-compatible Gemma models from:
     * https://www.kaggle.com/models/google/gemma/tfLite
     * 
     * Recommended models:
     * - gemma-2b-it-cpu-int4.task (~1.2GB) - For devices with 4GB RAM
     * - gemma-2b-it-cpu-int8.task (~2.3GB) - Better quality, needs 6GB+ RAM
     * 
     * After downloading, place in: app/src/main/assets/models/
     */
    val modelPath: String = "models/gemma2-2b-it-cpu-int8.task",  // Updated to installed model file
    
    /** Maximum number of tokens to generate */
    val maxTokens: Int = 512,
    
    /** Controls randomness in generation (0.0 = deterministic, 1.0 = very random) */
    val temperature: Float = 0.8f,
    
    /** Number of highest probability vocabulary tokens to keep for top-k sampling */
    val topK: Int = 40,
    
    /** Random seed for reproducible generation */
    val randomSeed: Int = 42,
    
    /** Timeout for inference operation in milliseconds (default: 30 seconds) */
    val inferenceTimeoutMs: Long = 30_000L,
    
    /** Whether to enable GPU acceleration (if available) */
    val enableGpuAcceleration: Boolean = true,
    
    /** Minimum device RAM required in MB */
    val minRequiredRamMB: Int = 3072,
    
    /** Whether to show performance metrics in logs */
    val enableMetrics: Boolean = true
)
