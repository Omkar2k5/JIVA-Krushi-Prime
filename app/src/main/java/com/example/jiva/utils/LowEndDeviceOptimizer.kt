package com.example.jiva.utils

import androidx.compose.runtime.*
import kotlinx.coroutines.*
import timber.log.Timber
import kotlin.math.min

/**
 * Low-End Device Optimizer for Stock Report
 * Handles large datasets efficiently on low-end devices
 */
object LowEndDeviceOptimizer {
    
    // Optimal settings for low-end devices
    private const val PAGE_SIZE = 50 // Small pages for memory efficiency
    private const val MAX_VISIBLE_ITEMS = 100 // Maximum items to show at once
    private const val FILTER_CHUNK_SIZE = 25 // Process filters in small chunks
    
    /**
     * Paginated Data State for efficient memory management
     */
    @Stable
    data class PaginatedDataState<T>(
        val currentPage: Int = 0,
        val pageSize: Int = PAGE_SIZE,
        val totalItems: Int = 0,
        val visibleItems: List<T> = emptyList(),
        val isLoading: Boolean = false,
        val hasMorePages: Boolean = false,
        val filterProgress: Float = 0f,
        val filterMessage: String = ""
    )
    
    /**
     * Create paginated data manager for Stock entries
     */
    @Composable
    fun <T> rememberPaginatedData(
        allData: List<T>,
        pageSize: Int = PAGE_SIZE,
        filterPredicate: (T) -> Boolean = { true }
    ): PaginatedDataState<T> {
        
        var state by remember { mutableStateOf(PaginatedDataState<T>()) }
        var filteredData by remember { mutableStateOf<List<T>>(emptyList()) }
        
        // Filter data in background with progress
        LaunchedEffect(allData, filterPredicate) {
            if (allData.isEmpty()) {
                state = PaginatedDataState()
                return@LaunchedEffect
            }
            
            state = state.copy(isLoading = true, filterProgress = 0f, filterMessage = "Starting filter...")
            
            try {
                // Filter data in chunks to prevent UI blocking
                val filtered = mutableListOf<T>()
                val chunks = allData.chunked(FILTER_CHUNK_SIZE)
                
                chunks.forEachIndexed { index, chunk ->
                    // Process chunk in background
                    withContext(Dispatchers.Default) {
                        val chunkFiltered = chunk.filter(filterPredicate)
                        filtered.addAll(chunkFiltered)
                    }
                    
                    // Update progress
                    val progress = ((index + 1).toFloat() / chunks.size) * 100f
                    state = state.copy(
                        filterProgress = progress,
                        filterMessage = "Filtering... ${index + 1}/${chunks.size} chunks"
                    )
                    
                    // Small delay to keep UI responsive
                    delay(10)
                }
                
                filteredData = filtered
                
                // Load first page
                val firstPageData = filtered.take(min(pageSize, MAX_VISIBLE_ITEMS))
                state = PaginatedDataState(
                    currentPage = 0,
                    pageSize = pageSize,
                    totalItems = filtered.size,
                    visibleItems = firstPageData,
                    isLoading = false,
                    hasMorePages = filtered.size > pageSize,
                    filterProgress = 100f,
                    filterMessage = "Complete: ${filtered.size} items"
                )
                
            } catch (e: Exception) {
                Timber.e(e, "Error during filtering")
                state = state.copy(
                    isLoading = false,
                    filterProgress = 100f,
                    filterMessage = "Error: ${e.message}"
                )
            }
        }
        
        return state
    }
    
    /**
     * Load next page of data
     */
    suspend fun <T> loadNextPage(
        currentState: PaginatedDataState<T>,
        allFilteredData: List<T>
    ): PaginatedDataState<T> {
        if (!currentState.hasMorePages || currentState.isLoading) {
            return currentState
        }
        
        return try {
            val nextPage = currentState.currentPage + 1
            val startIndex = nextPage * currentState.pageSize
            val endIndex = min(startIndex + currentState.pageSize, allFilteredData.size)
            
            if (startIndex >= allFilteredData.size) {
                return currentState.copy(hasMorePages = false)
            }
            
            val newItems = allFilteredData.subList(startIndex, endIndex)
            val allVisibleItems = currentState.visibleItems + newItems
            
            // Limit total visible items for memory management
            val limitedVisibleItems = if (allVisibleItems.size > MAX_VISIBLE_ITEMS) {
                allVisibleItems.takeLast(MAX_VISIBLE_ITEMS)
            } else {
                allVisibleItems
            }
            
            currentState.copy(
                currentPage = nextPage,
                visibleItems = limitedVisibleItems,
                hasMorePages = endIndex < allFilteredData.size
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error loading next page")
            currentState
        }
    }
    
    /**
     * Memory optimization for low-end devices
     */
    fun optimizeForLowEndDevice() {
        try {
            // Force garbage collection
            System.gc()
            
            // Get memory info
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
            
            Timber.d("📱 Memory optimization: ${memoryUsagePercent.toInt()}% used")
            
            // Log warning if memory usage is high
            if (memoryUsagePercent > 80f) {
                Timber.w("⚠️ High memory usage detected: ${memoryUsagePercent.toInt()}%")
            }
            
        } catch (e: Exception) {
            Timber.e(e, "Error during memory optimization")
        }
    }
    
    /**
     * Check if device is low-end based on available memory
     */
    fun isLowEndDevice(): Boolean {
        return try {
            val runtime = Runtime.getRuntime()
            val maxMemory = runtime.maxMemory()
            val maxMemoryMB = maxMemory / (1024 * 1024)
            
            // Consider devices with less than 512MB available heap as low-end
            val isLowEnd = maxMemoryMB < 512
            
            Timber.d("📱 Device memory: ${maxMemoryMB}MB, Low-end: $isLowEnd")
            isLowEnd
            
        } catch (e: Exception) {
            Timber.e(e, "Error checking device capabilities")
            true // Assume low-end if we can't determine
        }
    }
    
    /**
     * Get optimal settings based on device capabilities
     */
    fun getOptimalSettings(): OptimalSettings {
        val isLowEnd = isLowEndDevice()
        
        return if (isLowEnd) {
            OptimalSettings(
                pageSize = 25,
                maxVisibleItems = 50,
                filterChunkSize = 15,
                enableVirtualScrolling = true,
                enableProgressiveLoading = true,
                message = "Low-end device detected - optimized settings applied"
            )
        } else {
            OptimalSettings(
                pageSize = 50,
                maxVisibleItems = 100,
                filterChunkSize = 25,
                enableVirtualScrolling = false,
                enableProgressiveLoading = false,
                message = "Standard device - normal settings applied"
            )
        }
    }
    
    /**
     * Optimal settings data class
     */
    data class OptimalSettings(
        val pageSize: Int,
        val maxVisibleItems: Int,
        val filterChunkSize: Int,
        val enableVirtualScrolling: Boolean,
        val enableProgressiveLoading: Boolean,
        val message: String
    )
    
    /**
     * Emergency memory cleanup
     */
    fun emergencyMemoryCleanup() {
        try {
            Timber.w("🚨 Emergency memory cleanup initiated")
            
            // Force multiple garbage collections
            repeat(3) {
                System.gc()
                Thread.sleep(100)
            }
            
            // Log memory status after cleanup
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val memoryUsagePercent = (usedMemory.toFloat() / maxMemory.toFloat()) * 100f
            
            Timber.d("🧹 Memory after cleanup: ${memoryUsagePercent.toInt()}% used")
            
        } catch (e: Exception) {
            Timber.e(e, "Error during emergency memory cleanup")
        }
    }
}
