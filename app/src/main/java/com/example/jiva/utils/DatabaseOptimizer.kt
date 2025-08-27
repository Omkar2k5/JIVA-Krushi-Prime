package com.example.jiva.utils

import com.example.jiva.data.database.JivaDatabase
import kotlinx.coroutines.*
import timber.log.Timber

/**
 * Database Optimization Helper
 * Provides various optimization techniques for better performance
 */
object DatabaseOptimizer {
    
    /**
     * Optimize database for better performance
     */
    suspend fun optimizeDatabase(database: JivaDatabase) = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔧 Starting database optimization...")
            
            // Run VACUUM to reclaim space and optimize
            database.openHelper.writableDatabase.execSQL("VACUUM")
            Timber.d("✅ Database VACUUM completed")
            
            // Analyze tables for better query planning
            database.openHelper.writableDatabase.execSQL("ANALYZE")
            Timber.d("✅ Database ANALYZE completed")
            
            // Set optimal PRAGMA settings for performance
            val pragmaSettings = listOf(
                "PRAGMA journal_mode=WAL",           // Write-Ahead Logging for better concurrency
                "PRAGMA synchronous=NORMAL",        // Balance between safety and performance
                "PRAGMA cache_size=10000",          // Increase cache size (10MB)
                "PRAGMA temp_store=MEMORY",         // Store temp tables in memory
                "PRAGMA mmap_size=268435456",       // 256MB memory-mapped I/O
                "PRAGMA optimize"                   // Auto-optimize
            )
            
            pragmaSettings.forEach { pragma ->
                database.openHelper.writableDatabase.execSQL(pragma)
                Timber.d("✅ Applied: $pragma")
            }
            
            Timber.d("🚀 Database optimization completed successfully")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error during database optimization")
        }
    }
    
    /**
     * Create indexes for better query performance
     */
    suspend fun createPerformanceIndexes(database: JivaDatabase) = withContext(Dispatchers.IO) {
        try {
            Timber.d("📊 Creating performance indexes...")
            
            val indexes = listOf(
                // Outstanding table indexes
                "CREATE INDEX IF NOT EXISTS idx_outstanding_year_balance ON Outstanding(YearString, balance)",
                "CREATE INDEX IF NOT EXISTS idx_outstanding_account_name ON Outstanding(accountName)",
                "CREATE INDEX IF NOT EXISTS idx_outstanding_mobile ON Outstanding(mobile)",
                "CREATE INDEX IF NOT EXISTS idx_outstanding_days ON Outstanding(days)",
                
                // Stock table indexes
                "CREATE INDEX IF NOT EXISTS idx_stock_year_item ON tb_stock(YearString, Item_Name)",
                "CREATE INDEX IF NOT EXISTS idx_stock_company ON tb_stock(Company)",
                "CREATE INDEX IF NOT EXISTS idx_stock_itemtype ON tb_stock(ItemType)",
                "CREATE INDEX IF NOT EXISTS idx_stock_valuation ON tb_stock(Valuation)",
                "CREATE INDEX IF NOT EXISTS idx_stock_closing ON tb_stock(Closing_Stock)",
                
                // Composite indexes for common queries
                "CREATE INDEX IF NOT EXISTS idx_outstanding_composite ON Outstanding(YearString, accountName, balance)",
                "CREATE INDEX IF NOT EXISTS idx_stock_composite ON tb_stock(YearString, Item_Name, Company)"
            )
            
            indexes.forEach { indexSql ->
                try {
                    database.openHelper.writableDatabase.execSQL(indexSql)
                    Timber.d("✅ Created index: ${indexSql.substringAfter("idx_").substringBefore(" ON")}")
                } catch (e: Exception) {
                    Timber.w("⚠️ Index already exists or error: ${e.message}")
                }
            }
            
            Timber.d("📊 Performance indexes creation completed")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error creating performance indexes")
        }
    }
    
    /**
     * Get database statistics for monitoring
     */
    suspend fun getDatabaseStats(database: JivaDatabase): DatabaseStats = withContext(Dispatchers.IO) {
        try {
            val db = database.openHelper.readableDatabase
            
            // Get table sizes
            val outstandingCount = db.rawQuery("SELECT COUNT(*) FROM Outstanding", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            val stockCount = db.rawQuery("SELECT COUNT(*) FROM tb_stock", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            // Get database size
            val dbSize = db.rawQuery("PRAGMA page_count", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            val pageSize = db.rawQuery("PRAGMA page_size", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }

            val totalSizeBytes = dbSize * pageSize
            val totalSizeMB = totalSizeBytes / (1024 * 1024)

            // Get cache info
            val cacheSize = db.rawQuery("PRAGMA cache_size", arrayOf<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getInt(0) else 0
            }
            
            DatabaseStats(
                outstandingRecords = outstandingCount,
                stockRecords = stockCount,
                totalSizeMB = totalSizeMB,
                cacheSize = cacheSize,
                pageSize = pageSize
            )
            
        } catch (e: Exception) {
            Timber.e(e, "Error getting database stats")
            DatabaseStats()
        }
    }
    
    /**
     * Cleanup old data to improve performance
     */
    suspend fun cleanupOldData(database: JivaDatabase, keepYears: List<String>) = withContext(Dispatchers.IO) {
        try {
            Timber.d("🧹 Starting database cleanup...")
            
            val yearsToKeep = keepYears.joinToString("','", "'", "'")
            
            // Clean Outstanding data
            val outstandingDeleted = database.openHelper.writableDatabase.delete(
                "Outstanding",
                "YearString NOT IN ($yearsToKeep)",
                null
            )
            
            // Clean Stock data
            val stockDeleted = database.openHelper.writableDatabase.delete(
                "tb_stock",
                "YearString NOT IN ($yearsToKeep)",
                null
            )
            
            Timber.d("🧹 Cleanup completed: Outstanding: $outstandingDeleted, Stock: $stockDeleted records removed")
            
            // Optimize after cleanup
            optimizeDatabase(database)
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error during database cleanup")
        }
    }
    
    /**
     * Memory optimization for large datasets
     */
    fun optimizeMemoryUsage() {
        try {
            // Suggest garbage collection
            System.gc()
            
            // Get memory info
            val runtime = Runtime.getRuntime()
            val usedMemory = runtime.totalMemory() - runtime.freeMemory()
            val maxMemory = runtime.maxMemory()
            val availableMemory = maxMemory - usedMemory
            
            Timber.d("💾 Memory optimization: Used: ${usedMemory / 1024 / 1024}MB, Available: ${availableMemory / 1024 / 1024}MB")
            
        } catch (e: Exception) {
            Timber.e(e, "Error during memory optimization")
        }
    }
    
    /**
     * Initialize all optimizations
     */
    suspend fun initializeOptimizations(database: JivaDatabase) {
        try {
            Timber.d("🚀 Initializing all database optimizations...")
            
            // Run optimizations in parallel
            coroutineScope {
                launch { createPerformanceIndexes(database) }
                launch { optimizeDatabase(database) }
            }
            
            // Memory optimization
            optimizeMemoryUsage()
            
            // Log stats
            val stats = getDatabaseStats(database)
            Timber.d("📊 Database ready: ${stats.outstandingRecords} Outstanding, ${stats.stockRecords} Stock records, ${stats.totalSizeMB}MB")
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error during optimization initialization")
        }
    }
}

/**
 * Database statistics data class
 */
data class DatabaseStats(
    val outstandingRecords: Int = 0,
    val stockRecords: Int = 0,
    val totalSizeMB: Int = 0,
    val cacheSize: Int = 0,
    val pageSize: Int = 0
) {
    override fun toString(): String {
        return "Outstanding: $outstandingRecords, Stock: $stockRecords, Size: ${totalSizeMB}MB, Cache: $cacheSize"
    }
}
