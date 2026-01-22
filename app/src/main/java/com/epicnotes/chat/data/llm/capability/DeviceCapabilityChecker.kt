package com.epicnotes.chat.data.llm.capability

/**
 * Result of device capability check.
 */
data class DeviceCheck(
    val isCapable: Boolean,
    val warning: String?
)

/**
 * Interface for checking device capabilities for LLM operations.
 * Verifies device meets minimum requirements for on-device inference.
 */
interface DeviceCapabilityChecker {

    /**
     * Checks if device has sufficient memory for LLM operations.
     * @return DeviceCheck result indicating if device is capable and any warnings
     */
    fun checkCapability(): DeviceCheck

    /**
     * Checks if system is under memory pressure.
     * @return true if memory is critically low
     */
    fun isMemoryLow(): Boolean
}
