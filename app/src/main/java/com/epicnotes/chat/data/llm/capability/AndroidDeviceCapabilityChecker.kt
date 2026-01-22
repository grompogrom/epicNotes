package com.epicnotes.chat.data.llm.capability

import android.app.ActivityManager
import android.content.Context
import android.util.Log

/**
 * Android implementation of DeviceCapabilityChecker.
 * Checks device memory and system state for LLM operations.
 */
class AndroidDeviceCapabilityChecker(
    private val context: Context
) : DeviceCapabilityChecker {

    companion object {
        private const val TAG = "DeviceCapabilityChecker"
        private const val MIN_RAM_3GB = 3072
        private const val RECOMMENDED_RAM_4GB = 4096
    }

    private val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

    override fun checkCapability(): DeviceCheck {
        val totalMemoryMB = getTotalMemoryMB()

        return when {
            totalMemoryMB < MIN_RAM_3GB -> DeviceCheck(
                isCapable = false,
                warning = "Device has ${totalMemoryMB}MB RAM. Minimum 3GB recommended. App may crash or run slowly."
            )
            totalMemoryMB < RECOMMENDED_RAM_4GB -> DeviceCheck(
                isCapable = true,
                warning = "Device has ${totalMemoryMB}MB RAM. 4GB+ recommended for better performance."
            )
            else -> DeviceCheck(
                isCapable = true,
                warning = null
            )
        }
    }

    override fun isMemoryLow(): Boolean {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return memInfo.lowMemory
    }

    private fun getTotalMemoryMB(): Int {
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)
        return (memInfo.totalMem / (1024 * 1024)).toInt()
    }
}
