package com.epicnotes.chat.data.llm.file

import com.epicnotes.chat.data.llm.error.ModelInitializationException
import java.io.File

/**
 * Interface for managing model file operations.
 * Handles file location, copying from assets, validation, and verification.
 */
interface ModelFileManager {

    /**
     * Gets the path to the model file for MediaPipe.
     * @param assetPath Path to model in assets
     * @return Absolute path to model file in internal storage
     */
    suspend fun getModelPath(assetPath: String): String

    /**
     * Checks if the model file exists in internal storage.
     * @param assetPath Path to model in assets
     * @return true if file exists and is not empty
     */
    suspend fun modelExists(assetPath: String): Boolean

    /**
     * Copies model file from assets to internal storage if not already present.
     * @param assetPath Path to model in assets
     * @throws ModelInitializationException if copy fails or file not found in assets
     */
    suspend fun copyFromAssetsIfNeeded(assetPath: String)

    /**
     * Validates that the model file is readable and not corrupted.
     * @param file The model file to validate
     * @throws ModelInitializationException if file is invalid
     */
    suspend fun validateFile(file: File)

    /**
     * Verifies the model file size matches expected size (if provided).
     * @param file The model file to verify
     * @param expectedSize Expected file size in bytes, null to skip verification
     * @throws ModelInitializationException if size doesn't match
     */
    suspend fun verifyFileSize(file: File, expectedSize: Long?)
}
