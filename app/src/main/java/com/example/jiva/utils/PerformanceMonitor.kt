package com.example.jiva.utils

import timber.log.Timber
import kotlin.system.measureTimeMillis

/**
 * Performance monitoring utility for tracking app performance
 * Useful for monitoring performance with 100+ concurrent users
 */
object PerformanceMonitor {
    
    private const val SLOW_OPERATION_THRESHOLD_MS = 1000L
    private const val VERY_SLOW_OPERATION_THRESHOLD_MS = 3000L
    
    /**
     * Measure execution time of a suspend function
     */
    suspend inline fun <T> measureSuspend(
        operationName: String,
        operation: suspend () -> T
    ): T {
        var result: T
        val timeMs = measureTimeMillis {
            result = operation()
        }
        
        logPerformance(operationName, timeMs)
        return result
    }
    
    /**
     * Measure execution time of a regular function
     */
    inline fun <T> measure(
        operationName: String,
        operation: () -> T
    ): T {
        var result: T
        val timeMs = measureTimeMillis {
            result = operation()
        }
        
        logPerformance(operationName, timeMs)
        return result
    }
    
    /**
     * Log performance metrics with appropriate log levels
     */
    private fun logPerformance(operationName: String, timeMs: Long) {
        when {
            timeMs >= VERY_SLOW_OPERATION_THRESHOLD_MS -> {
                Timber.w("VERY SLOW: $operationName took ${timeMs}ms")
            }
            timeMs >= SLOW_OPERATION_THRESHOLD_MS -> {
                Timber.w("SLOW: $operationName took ${timeMs}ms")
            }
            else -> {
                Timber.d("$operationName completed in ${timeMs}ms")
            }
        }
    }
    
    /**
     * Track memory usage
     */
    fun logMemoryUsage(context: String) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val availableMemory = maxMemory - usedMemory
        
        val usedMB = usedMemory / (1024 * 1024)
        val maxMB = maxMemory / (1024 * 1024)
        val availableMB = availableMemory / (1024 * 1024)
        
        Timber.d("Memory [$context]: Used: ${usedMB}MB, Max: ${maxMB}MB, Available: ${availableMB}MB")
        
        // Warn if memory usage is high
        val memoryUsagePercent = (usedMemory.toDouble() / maxMemory.toDouble()) * 100
        if (memoryUsagePercent > 80) {
            Timber.w("HIGH MEMORY USAGE [$context]: ${memoryUsagePercent.toInt()}%")
        }
    }
    
    /**
     * Track network operation performance
     */
    suspend inline fun <T> measureNetworkOperation(
        operationName: String,
        operation: suspend () -> T
    ): T {
        Timber.d("Starting network operation: $operationName")
        return measureSuspend("Network: $operationName", operation)
    }
    
    /**
     * Track database operation performance
     */
    suspend inline fun <T> measureDatabaseOperation(
        operationName: String,
        operation: suspend () -> T
    ): T {
        Timber.d("Starting database operation: $operationName")
        return measureSuspend("Database: $operationName", operation)
    }
    
    /**
     * Track UI operation performance
     */
    inline fun <T> measureUIOperation(
        operationName: String,
        operation: () -> T
    ): T {
        return measure("UI: $operationName", operation)
    }
}
