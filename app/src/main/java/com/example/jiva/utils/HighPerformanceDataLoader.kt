package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.OutstandingEntity
import com.example.jiva.data.database.entities.StockEntity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * High-Performance Data Loader with Multithreading and Progressive Loading
 * Optimized for large datasets with smooth UX
 */
object HighPerformanceDataLoader {
    
    // Thread pools for different operations
    private val ioDispatcher = Dispatchers.IO.limitedParallelism(4)
    private val computationDispatcher = Dispatchers.Default.limitedParallelism(2)
    
    // Cache for processed data
    private val dataCache = ConcurrentHashMap<String, Any>()
    private val loadingStates = ConcurrentHashMap<String, Boolean>()
    
    /**
     * Load Outstanding data with progressive loading and multithreading
     */
    suspend fun loadOutstandingDataProgressive(
        database: JivaDatabase,
        year: String,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> },
        onBatch: (List<OutstandingEntity>) -> Unit = { }
    ): Flow<List<OutstandingEntity>> = flow {
        val cacheKey = "outstanding_$year"
        
        try {
            onProgress(0, 100, "Initializing Outstanding data loading...")
            
            // Check cache first
            @Suppress("UNCHECKED_CAST")
            val cachedData = dataCache[cacheKey] as? List<OutstandingEntity>
            if (cachedData != null) {
                onProgress(100, 100, "Loading from cache...")
                emit(cachedData)
                return@flow
            }
            
            onProgress(10, 100, "Querying database...")
            
            // Load data in background thread
            val allData = withContext(ioDispatcher) {
                database.outstandingDao().getAllSync(year)
            }
            
            if (allData.isEmpty()) {
                onProgress(100, 100, "No data found")
                emit(emptyList())
                return@flow
            }
            
            onProgress(30, 100, "Processing ${allData.size} records...")
            
            // Process data in chunks for smooth UX
            val chunkSize = 50 // Smaller chunks for smoother loading
            val totalChunks = (allData.size + chunkSize - 1) / chunkSize
            val processedData = mutableListOf<OutstandingEntity>()
            
            allData.chunked(chunkSize).forEachIndexed { index, chunk ->
                // Process chunk in computation thread
                val processedChunk = withContext(computationDispatcher) {
                    chunk.map { entity ->
                        // Optimize entity processing
                        entity.copy(
                            balance = entity.balance.trim(),
                            accountName = entity.accountName.trim(),
                            mobile = entity.mobile.trim()
                        )
                    }
                }
                
                processedData.addAll(processedChunk)
                
                // Emit progressive updates
                onBatch(processedChunk)
                emit(processedData.toList())
                
                val progress = ((index + 1) * 70 / totalChunks) + 30
                onProgress(progress, 100, "Processed ${processedData.size}/${allData.size} records")
                
                // Small delay to keep UI responsive
                delay(10)
            }
            
            // Cache the result
            dataCache[cacheKey] = processedData
            onProgress(100, 100, "Completed: ${processedData.size} records loaded")
            
        } catch (e: Exception) {
            Timber.e(e, "Error in progressive Outstanding data loading")
            onProgress(100, 100, "Error: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)
    
    /**
     * Load Stock data with progressive loading and multithreading
     */
    suspend fun loadStockDataProgressive(
        database: JivaDatabase,
        year: String,
        onProgress: (Int, Int, String) -> Unit = { _, _, _ -> },
        onBatch: (List<StockEntity>) -> Unit = { }
    ): Flow<List<StockEntity>> = flow {
        val cacheKey = "stock_$year"
        
        try {
            onProgress(0, 100, "Initializing Stock data loading...")
            
            // Check cache first
            @Suppress("UNCHECKED_CAST")
            val cachedData = dataCache[cacheKey] as? List<StockEntity>
            if (cachedData != null) {
                onProgress(100, 100, "Loading from cache...")
                emit(cachedData)
                return@flow
            }
            
            onProgress(10, 100, "Querying database...")
            
            // Load data in background thread
            val allData = withContext(ioDispatcher) {
                database.stockDao().getAllSync(year)
            }
            
            if (allData.isEmpty()) {
                onProgress(100, 100, "No data found")
                emit(emptyList())
                return@flow
            }
            
            onProgress(30, 100, "Processing ${allData.size} records...")
            
            // Process data in chunks for smooth UX
            val chunkSize = 30 // Smaller chunks for Stock (more complex data)
            val totalChunks = (allData.size + chunkSize - 1) / chunkSize
            val processedData = mutableListOf<StockEntity>()
            
            allData.chunked(chunkSize).forEachIndexed { index, chunk ->
                // Process chunk in computation thread
                val processedChunk = withContext(computationDispatcher) {
                    chunk.map { entity ->
                        // Optimize entity processing - strings are already optimized
                        entity.copy(
                            itemName = entity.itemName.trim(),
                            company = entity.company.trim(),
                            itemType = entity.itemType.trim()
                        )
                    }
                }
                
                processedData.addAll(processedChunk)
                
                // Emit progressive updates
                onBatch(processedChunk)
                emit(processedData.toList())
                
                val progress = ((index + 1) * 70 / totalChunks) + 30
                onProgress(progress, 100, "Processed ${processedData.size}/${allData.size} records")
                
                // Small delay to keep UI responsive
                delay(15) // Slightly longer for Stock data
            }
            
            // Cache the result
            dataCache[cacheKey] = processedData
            onProgress(100, 100, "Completed: ${processedData.size} records loaded")
            
        } catch (e: Exception) {
            Timber.e(e, "Error in progressive Stock data loading")
            onProgress(100, 100, "Error: ${e.message}")
            emit(emptyList())
        }
    }.flowOn(ioDispatcher)
    
    /**
     * Paginated data loading for extremely large datasets
     */
    suspend fun loadDataPaginated<T>(
        loadPage: suspend (offset: Int, limit: Int) -> List<T>,
        pageSize: Int = 100,
        onProgress: (Int, String) -> Unit = { _, _ -> }
    ): Flow<List<T>> = flow {
        val allData = mutableListOf<T>()
        var offset = 0
        var hasMore = true
        var pageNumber = 1
        
        while (hasMore) {
            onProgress(pageNumber, "Loading page $pageNumber...")
            
            val pageData = withContext(ioDispatcher) {
                loadPage(offset, pageSize)
            }
            
            if (pageData.isEmpty()) {
                hasMore = false
            } else {
                allData.addAll(pageData)
                emit(allData.toList())
                
                offset += pageSize
                pageNumber++
                
                // Small delay between pages
                delay(20)
            }
        }
        
        onProgress(pageNumber, "Completed: ${allData.size} total records")
    }.flowOn(ioDispatcher)
    
    /**
     * Clear cache for memory management
     */
    fun clearCache(key: String? = null) {
        if (key != null) {
            dataCache.remove(key)
        } else {
            dataCache.clear()
        }
        Timber.d("Data cache cleared: ${key ?: "all"}")
    }
    
    /**
     * Get cache size for monitoring
     */
    fun getCacheInfo(): String {
        val totalSize = dataCache.values.sumOf { 
            when (it) {
                is List<*> -> it.size
                else -> 1
            }
        }
        return "Cache entries: ${dataCache.size}, Total records: $totalSize"
    }
    
    /**
     * Preload data in background
     */
    fun preloadData(
        database: JivaDatabase,
        year: String,
        scope: CoroutineScope
    ) {
        scope.launch(ioDispatcher) {
            try {
                Timber.d("🚀 Preloading data for year: $year")
                
                // Preload both Outstanding and Stock data
                launch {
                    loadOutstandingDataProgressive(database, year).collect { }
                }
                launch {
                    loadStockDataProgressive(database, year).collect { }
                }
                
                Timber.d("✅ Data preloading completed")
            } catch (e: Exception) {
                Timber.e(e, "❌ Error during data preloading")
            }
        }
    }
}
