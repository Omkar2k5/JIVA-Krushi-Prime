package com.example.jiva.utils

import android.content.Context
import android.os.Build
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Performance monitoring and optimization utilities
 * Compatible with Android 7-15
 */
object PerformanceUtils {
    
    /**
     * Memory usage information
     */
    data class MemoryInfo(
        val totalMemory: Long,
        val availableMemory: Long,
        val usedMemory: Long,
        val usedPercentage: Float
    )
    
    /**
     * Get current memory usage information
     */
    fun getMemoryInfo(context: Context): MemoryInfo {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        
        val totalMemory = memoryInfo.totalMem
        val availableMemory = memoryInfo.availMem
        val usedMemory = totalMemory - availableMemory
        val usedPercentage = (usedMemory.toFloat() / totalMemory.toFloat()) * 100
        
        return MemoryInfo(
            totalMemory = totalMemory / (1024 * 1024), // Convert to MB
            availableMemory = availableMemory / (1024 * 1024),
            usedMemory = usedMemory / (1024 * 1024),
            usedPercentage = usedPercentage
        )
    }
    
    /**
     * Check if device is low on memory
     */
    fun isLowMemory(context: Context): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.lowMemory
    }
    
    /**
     * Perform garbage collection if memory usage is high
     */
    fun performGCIfNeeded(context: Context, threshold: Float = 80f) {
        val memoryInfo = getMemoryInfo(context)
        if (memoryInfo.usedPercentage > threshold) {
            Timber.d("Memory usage high (${memoryInfo.usedPercentage}%), performing GC")
            System.gc()
        }
    }
    
    /**
     * Measure execution time of a block of code
     */
    inline fun <T> measureTime(operation: String, block: () -> T): T {
        val startTime = System.currentTimeMillis()
        val result = block()
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        if (duration > 100) { // Log only if operation takes more than 100ms
            Timber.d("$operation took ${duration}ms")
        }
        
        return result
    }
    
    /**
     * Check device performance characteristics
     */
    fun getDevicePerformanceInfo(): DevicePerformanceInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val isLowRamDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                val activityManager = android.app.ActivityManager::class.java
                val method = activityManager.getMethod("isLowRamDevice")
                method.invoke(null) as Boolean
            } catch (e: Exception) {
                false
            }
        } else {
            false
        }
        
        return DevicePerformanceInfo(
            cpuCores = cores,
            isLowRamDevice = isLowRamDevice,
            sdkVersion = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL
        )
    }
    
    /**
     * Device performance characteristics
     */
    data class DevicePerformanceInfo(
        val cpuCores: Int,
        val isLowRamDevice: Boolean,
        val sdkVersion: Int,
        val manufacturer: String,
        val model: String
    )
    
    /**
     * Composable function to monitor memory usage
     */
    @Composable
    fun rememberMemoryMonitor(context: Context, intervalMs: Long = 5000): State<MemoryInfo> {
        val memoryInfo = remember { mutableStateOf(getMemoryInfo(context)) }
        
        LaunchedEffect(Unit) {
            while (true) {
                delay(intervalMs)
                memoryInfo.value = getMemoryInfo(context)
                
                // Log warning if memory usage is high
                if (memoryInfo.value.usedPercentage > 85f) {
                    Timber.w("High memory usage: ${memoryInfo.value.usedPercentage}%")
                }
            }
        }
        
        return memoryInfo
    }
    
    /**
     * Optimize images for different screen densities
     */
    fun getOptimalImageSize(screenWidthDp: Int, screenHeightDp: Int): Pair<Int, Int> {
        return when {
            screenWidthDp < 600 -> Pair(400, 300) // Small screens
            screenWidthDp < 840 -> Pair(600, 450) // Medium screens
            screenWidthDp < 1200 -> Pair(800, 600) // Large screens
            else -> Pair(1200, 900) // Extra large screens
        }
    }
    
    /**
     * Check if device supports hardware acceleration
     */
    fun isHardwareAccelerated(context: Context): Boolean {
        return try {
            val activity = context as? android.app.Activity
            activity?.window?.attributes?.flags?.and(android.view.WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get recommended thread pool size based on device capabilities
     */
    fun getRecommendedThreadPoolSize(): Int {
        val cores = Runtime.getRuntime().availableProcessors()
        return when {
            cores <= 2 -> 2
            cores <= 4 -> 3
            cores <= 8 -> 4
            else -> 6
        }.coerceAtMost(8) // Cap at 8 threads
    }
    
    /**
     * Check if device is running on battery saver mode
     */
    fun isBatterySaverMode(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            powerManager.isPowerSaveMode
        } else {
            false
        }
    }
    
    /**
     * Log device and performance information
     */
    fun logDeviceInfo(context: Context) {
        val deviceInfo = getDevicePerformanceInfo()
        val memoryInfo = getMemoryInfo(context)
        
        Timber.i("Device Performance Info:")
        Timber.i("  CPU Cores: ${deviceInfo.cpuCores}")
        Timber.i("  Low RAM Device: ${deviceInfo.isLowRamDevice}")
        Timber.i("  SDK Version: ${deviceInfo.sdkVersion}")
        Timber.i("  Manufacturer: ${deviceInfo.manufacturer}")
        Timber.i("  Model: ${deviceInfo.model}")
        Timber.i("  Total Memory: ${memoryInfo.totalMemory} MB")
        Timber.i("  Available Memory: ${memoryInfo.availableMemory} MB")
        Timber.i("  Memory Usage: ${memoryInfo.usedPercentage}%")
        Timber.i("  Hardware Accelerated: ${isHardwareAccelerated(context)}")
        Timber.i("  Battery Saver: ${isBatterySaverMode(context)}")
    }
}
