package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Simple Performance Optimizer
 * Provides immediate performance improvements without complex flows
 */
object SimplePerformanceOptimizer {
    
    /**
     * Optimize app startup performance
     */
    fun optimizeAppStartup(
        context: Context,
        database: JivaDatabase,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                Timber.d("🚀 Starting simple performance optimization")
                
                // Basic database optimization
                optimizeDatabase(database)
                
                // Memory optimization
                optimizeMemory()
                
                Timber.d("✅ Simple performance optimization completed")
                
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during performance optimization")
            }
        }
    }
    
    /**
     * Basic database optimization
     */
    private suspend fun optimizeDatabase(database: JivaDatabase) = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.writableDatabase
            
            // Basic optimizations
            db.execSQL("PRAGMA journal_mode=WAL")
            db.execSQL("PRAGMA synchronous=NORMAL")
            db.execSQL("PRAGMA cache_size=5000")
            db.execSQL("PRAGMA temp_store=MEMORY")
            
            Timber.d("✅ Basic database optimization completed")
            
        } catch (e: Exception) {
            Timber.e(e, "Error in basic database optimization")
        }
    }
    
    /**
     * Memory optimization
     */
    private fun optimizeMemory() {
        try {
            // Suggest garbage collection
            System.gc()
            
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            
            Timber.d("💾 Memory optimized: Used ${usedMemory / 1024 / 1024}MB / Max ${maxMemory / 1024 / 1024}MB")
            
        } catch (e: Exception) {
            Timber.e(e, "Error in memory optimization")
        }
    }
    
    /**
     * Chunked data processing for large datasets
     */
    suspend fun <T, R> processDataInChunks(
        data: List<T>,
        chunkSize: Int = 50,
        processor: suspend (List<T>) -> List<R>,
        onProgress: (Int, Int) -> Unit = { _, _ -> }
    ): List<R> = withContext(Dispatchers.Default) {
        val result = mutableListOf<R>()
        val chunks = data.chunked(chunkSize)
        
        chunks.forEachIndexed { index, chunk ->
            try {
                val processed = processor(chunk)
                result.addAll(processed)
                
                onProgress(index + 1, chunks.size)
                
                // Small delay to keep UI responsive
                delay(10)
                
            } catch (e: Exception) {
                Timber.e(e, "Error processing chunk $index")
            }
        }
        
        result
    }
    
    /**
     * Background data preloading
     */
    fun preloadDataInBackground(
        database: JivaDatabase,
        year: String,
        scope: CoroutineScope
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                Timber.d("🔄 Preloading data in background for year: $year")
                
                // Preload Outstanding data
                launch {
                    val outstanding = database.outstandingDao().getAllSync(year)
                    Timber.d("📊 Preloaded ${outstanding.size} Outstanding records")
                }
                
                // Preload Stock data
                launch {
                    val stock = database.stockDao().getAllSync(year)
                    Timber.d("📦 Preloaded ${stock.size} Stock records")
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Error during background preloading")
            }
        }
    }
    
    /**
     * Optimize UI rendering for large lists
     */
    fun getOptimalChunkSize(dataSize: Int): Int {
        return when {
            dataSize < 100 -> dataSize // Show all for small datasets
            dataSize < 500 -> 50       // Medium chunks for medium datasets
            dataSize < 1000 -> 30      // Smaller chunks for large datasets
            else -> 20                 // Very small chunks for huge datasets
        }
    }
    
    /**
     * Check if device has sufficient memory for large operations
     */
    fun hasEnoughMemoryForLargeDataset(dataSize: Int): Boolean {
        val runtime = Runtime.getRuntime()
        val availableMemory = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        val estimatedMemoryNeeded = dataSize * 1024L // Rough estimate: 1KB per record
        
        return availableMemory > estimatedMemoryNeeded * 2 // 2x safety margin
    }
    
    /**
     * Get performance recommendations based on data size
     */
    fun getPerformanceRecommendations(dataSize: Int): PerformanceRecommendation {
        return when {
            dataSize < 100 -> PerformanceRecommendation(
                strategy = "DIRECT_LOAD",
                chunkSize = dataSize,
                useVirtualScrolling = false,
                message = "Small dataset - direct loading recommended"
            )
            dataSize < 500 -> PerformanceRecommendation(
                strategy = "CHUNKED_LOAD",
                chunkSize = 50,
                useVirtualScrolling = false,
                message = "Medium dataset - chunked loading recommended"
            )
            dataSize < 1000 -> PerformanceRecommendation(
                strategy = "CHUNKED_LOAD_WITH_PROGRESS",
                chunkSize = 30,
                useVirtualScrolling = true,
                message = "Large dataset - chunked loading with progress recommended"
            )
            else -> PerformanceRecommendation(
                strategy = "VIRTUAL_SCROLLING",
                chunkSize = 20,
                useVirtualScrolling = true,
                message = "Huge dataset - virtual scrolling required"
            )
        }
    }
}

/**
 * Performance recommendation data class
 */
data class PerformanceRecommendation(
    val strategy: String,
    val chunkSize: Int,
    val useVirtualScrolling: Boolean,
    val message: String
)
