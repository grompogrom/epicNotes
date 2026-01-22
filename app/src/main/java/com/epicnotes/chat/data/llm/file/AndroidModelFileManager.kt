package com.epicnotes.chat.data.llm.file

import android.content.Context
import android.util.Log
import com.epicnotes.chat.data.llm.error.ModelInitializationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of ModelFileManager.
 * Handles model file operations on Android internal storage.
 */
class AndroidModelFileManager(
    private val context: Context
) : ModelFileManager {

    companion object {
        private const val TAG = "ModelFileManager"
    }

    override suspend fun getModelPath(assetPath: String): String {
        val modelFileName = assetPath.substringAfterLast("/")
        val modelsDir = File(context.filesDir, "models")
        modelsDir.mkdirs()
        val modelFile = File(modelsDir, modelFileName)
        return modelFile.absolutePath
    }

    override suspend fun modelExists(assetPath: String): Boolean {
        val modelPath = getModelPath(assetPath)
        val modelFile = File(modelPath)
        return modelFile.exists() && modelFile.length() > 0L
    }

    override suspend fun copyFromAssetsIfNeeded(assetPath: String) {
        withContext(Dispatchers.IO) {
            if (modelExists(assetPath)) {
                Log.d(TAG, "Model already exists in internal storage")
                return@withContext
            }

            val modelPath = getModelPath(assetPath)
            val modelFile = File(modelPath)

            try {
                Log.i(TAG, "Copying model from assets: $assetPath")

                var expectedSize: Long? = null
                context.assets.open(assetPath).use { input ->
                    expectedSize = input.available().toLong()
                    Log.i(TAG, "Found model in assets (${expectedSize!! / (1024 * 1024)}MB), copying to internal storage...")

                    modelFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                }

                Log.i(TAG, "Model copied successfully (${modelFile.length() / (1024 * 1024)}MB)")

                if (expectedSize != null && modelFile.length() != expectedSize) {
                    throw ModelInitializationException(
                        "Model file size mismatch: expected $expectedSize bytes, got ${modelFile.length()} bytes. File may be corrupted."
                    )
                }
            } catch (e: java.io.FileNotFoundException) {
                val modelFileName = assetPath.substringAfterLast("/")
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
        }
    }

    override suspend fun validateFile(file: File) {
        withContext(Dispatchers.IO) {
            if (!file.exists() || file.length() == 0L) {
                throw ModelInitializationException("Model file not found or empty: ${file.absolutePath}")
            }

            try {
                file.inputStream().use { input ->
                    val header = ByteArray(4)
                    if (input.read(header) < 4) {
                        throw ModelInitializationException("Model file is too small or corrupted")
                    }
                }
            } catch (e: ModelInitializationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Failed to read model file: ${e.message}", e)
                throw ModelInitializationException("Model file is not readable: ${e.message}. File may be corrupted.", e)
            }
        }
    }

    override suspend fun verifyFileSize(file: File, expectedSize: Long?) {
        withContext(Dispatchers.IO) {
            val actualSize = file.length()

            if (expectedSize != null && actualSize != expectedSize) {
                throw ModelInitializationException(
                    "Model file size mismatch: expected $expectedSize bytes, got $actualSize bytes. File may be corrupted."
                )
            }

            if (actualSize == 0L) {
                throw ModelInitializationException("Model file is empty (0 bytes). File may be corrupted.")
            }
        }
    }
}
