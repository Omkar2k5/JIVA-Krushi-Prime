package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.repository.JivaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * API Data Manager - Handles all API calls and permanent storage
 * Architecture:
 * 1. API Call → Fetch from Server
 * 2. Store in Permanent Storage
 * 3. Frontend reads from Permanent Storage only
 */
object ApiDataManager {
    
    /**
     * Refresh Outstanding data - API → Permanent Storage
     */
    suspend fun refreshOutstandingData(
        context: Context,
        repository: JivaRepository,
        database: JivaDatabase,
        userId: Int,
        year: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting Outstanding API refresh for userId: $userId, year: $year")
            
            // 1. Call API
            val apiResult = repository.syncOutstanding(userId, year)
            
            if (apiResult.isSuccess) {
                // 2. Get data from Room DB (API stores data here first)
                val dbData = database.outstandingDao().getAllSync(year)
                
                // 3. Store in Permanent Storage
                val saveSuccess = PermanentStorageManager.saveOutstandingData(context, dbData, year)
                
                if (saveSuccess) {
                    Timber.d("✅ Outstanding data refreshed successfully: ${dbData.size} entries")
                    Result.success("Outstanding data refreshed: ${dbData.size} entries")
                } else {
                    Timber.e("❌ Failed to save Outstanding data to permanent storage")
                    Result.failure(Exception("Failed to save to permanent storage"))
                }
            } else {
                val error = apiResult.exceptionOrNull()?.message ?: "API call failed"
                Timber.e("❌ Outstanding API call failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error refreshing Outstanding data")
            Result.failure(e)
        }
    }
    
    /**
     * Refresh Stock data - API → Permanent Storage
     */
    suspend fun refreshStockData(
        context: Context,
        repository: JivaRepository,
        database: JivaDatabase,
        userId: Int,
        year: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting Stock API refresh for userId: $userId, year: $year")
            
            // 1. Call API
            val apiResult = repository.syncStock(userId, year)
            
            if (apiResult.isSuccess) {
                // 2. Get data from Room DB (API stores data here first)
                val dbData = database.stockDao().getAllSync(year)
                
                // 3. Store in Permanent Storage
                val saveSuccess = PermanentStorageManager.saveStockData(context, dbData, year)
                
                if (saveSuccess) {
                    Timber.d("✅ Stock data refreshed successfully: ${dbData.size} entries")
                    Result.success("Stock data refreshed: ${dbData.size} entries")
                } else {
                    Timber.e("❌ Failed to save Stock data to permanent storage")
                    Result.failure(Exception("Failed to save to permanent storage"))
                }
            } else {
                val error = apiResult.exceptionOrNull()?.message ?: "API call failed"
                Timber.e("❌ Stock API call failed: $error")
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error refreshing Stock data")
            Result.failure(e)
        }
    }
    
    /**
     * Refresh ALL data (for Home Screen refresh button)
     */
    suspend fun refreshAllData(
        context: Context,
        repository: JivaRepository,
        database: JivaDatabase,
        userId: Int,
        year: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🔄 Starting ALL DATA refresh for userId: $userId, year: $year")
            
            val results = mutableListOf<String>()
            val errors = mutableListOf<String>()
            
            // 1. Outstanding Data
            try {
                val outstandingResult = refreshOutstandingData(context, repository, database, userId, year)
                if (outstandingResult.isSuccess) {
                    results.add("Outstanding: ✅")
                } else {
                    errors.add("Outstanding: ❌ ${outstandingResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                errors.add("Outstanding: ❌ ${e.message}")
            }
            
            // 2. Stock Data
            try {
                val stockResult = refreshStockData(context, repository, database, userId, year)
                if (stockResult.isSuccess) {
                    results.add("Stock: ✅")
                } else {
                    errors.add("Stock: ❌ ${stockResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                errors.add("Stock: ❌ ${e.message}")
            }
            
            // 3. TODO: Add other 6 APIs here
            // - Accounts
            // - Ledger
            // - Sale/Purchase
            // - Expiry
            // - Templates
            // - Price Data
            
            val summary = buildString {
                appendLine("📊 Data Refresh Summary:")
                results.forEach { appendLine("  $it") }
                if (errors.isNotEmpty()) {
                    appendLine("❌ Errors:")
                    errors.forEach { appendLine("  $it") }
                }
            }
            
            Timber.d(summary)
            
            if (errors.isEmpty()) {
                Result.success("All data refreshed successfully")
            } else {
                Result.failure(Exception("Some data refresh failed: ${errors.size} errors"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Error refreshing all data")
            Result.failure(e)
        }
    }
    
    /**
     * Load Outstanding data from permanent storage (for UI)
     */
    suspend fun loadOutstandingDataForUI(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.OutstandingEntity> {
        return PermanentStorageManager.loadOutstandingData(context, year)
    }
    
    /**
     * Load Stock data from permanent storage (for UI)
     */
    suspend fun loadStockDataForUI(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.StockEntity> {
        return PermanentStorageManager.loadStockData(context, year)
    }
    
    /**
     * Check if data is available in permanent storage
     */
    suspend fun isDataAvailable(
        context: Context,
        dataType: String,
        year: String = ""
    ): Boolean {
        return PermanentStorageManager.isDataAvailable(context, dataType, year)
    }
}
