package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.repository.JivaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Home Screen API Manager
 * Handles the "Refresh All" functionality for all 8 screens
 * Architecture: Home Refresh → All APIs → Permanent Storage
 */
object HomeScreenApiManager {
    
    /**
     * Refresh ALL data from Home Screen
     * Calls all 8 APIs and stores data in permanent storage
     */
    suspend fun refreshAllScreensData(
        context: Context,
        repository: JivaRepository,
        database: JivaDatabase,
        userId: Int,
        year: String
    ): Result<RefreshSummary> = withContext(Dispatchers.IO) {
        try {
            Timber.d("🏠 Starting HOME SCREEN refresh for all 8 screens - userId: $userId, year: $year")
            
            val summary = RefreshSummary()
            
            // 1. Outstanding Report
            try {
                val result = ApiDataManager.refreshOutstandingData(context, repository, database, userId, year)
                if (result.isSuccess) {
                    summary.successfulScreens.add("Outstanding ✅")
                    Timber.d("✅ Outstanding refresh successful")
                } else {
                    summary.failedScreens.add("Outstanding ❌: ${result.exceptionOrNull()?.message}")
                    Timber.e("❌ Outstanding refresh failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                summary.failedScreens.add("Outstanding ❌: ${e.message}")
                Timber.e(e, "❌ Outstanding refresh exception")
            }
            
            // 2. Stock Report
            try {
                val result = ApiDataManager.refreshStockData(context, repository, database, userId, year)
                if (result.isSuccess) {
                    summary.successfulScreens.add("Stock ✅")
                    Timber.d("✅ Stock refresh successful")
                } else {
                    summary.failedScreens.add("Stock ❌: ${result.exceptionOrNull()?.message}")
                    Timber.e("❌ Stock refresh failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                summary.failedScreens.add("Stock ❌: ${e.message}")
                Timber.e(e, "❌ Stock refresh exception")
            }
            
            // 3. TODO: Account Master Report
            try {
                // TODO: Implement when AccountMaster API is ready
                summary.pendingScreens.add("Account Master (TODO)")
                Timber.d("⏳ Account Master API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Account Master ❌: ${e.message}")
            }
            
            // 4. TODO: Ledger Report
            try {
                // TODO: Implement when Ledger API is ready
                summary.pendingScreens.add("Ledger (TODO)")
                Timber.d("⏳ Ledger API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Ledger ❌: ${e.message}")
            }
            
            // 5. TODO: Sale/Purchase Report
            try {
                // TODO: Implement when Sale/Purchase API is ready
                summary.pendingScreens.add("Sale/Purchase (TODO)")
                Timber.d("⏳ Sale/Purchase API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Sale/Purchase ❌: ${e.message}")
            }
            
            // 6. TODO: Expiry Report
            try {
                // TODO: Implement when Expiry API is ready
                summary.pendingScreens.add("Expiry (TODO)")
                Timber.d("⏳ Expiry API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Expiry ❌: ${e.message}")
            }
            
            // 7. TODO: Templates
            try {
                // TODO: Implement when Templates API is ready
                summary.pendingScreens.add("Templates (TODO)")
                Timber.d("⏳ Templates API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Templates ❌: ${e.message}")
            }
            
            // 8. TODO: Price Data
            try {
                // TODO: Implement when Price Data API is ready
                summary.pendingScreens.add("Price Data (TODO)")
                Timber.d("⏳ Price Data API not implemented yet")
            } catch (e: Exception) {
                summary.failedScreens.add("Price Data ❌: ${e.message}")
            }
            
            // Generate summary
            val totalScreens = 8
            val successCount = summary.successfulScreens.size
            val failedCount = summary.failedScreens.size
            val pendingCount = summary.pendingScreens.size
            
            summary.overallMessage = "📊 Refresh Summary: $successCount✅ $failedCount❌ $pendingCount⏳ (Total: $totalScreens)"
            
            Timber.d(summary.overallMessage)
            summary.successfulScreens.forEach { Timber.d("  $it") }
            summary.failedScreens.forEach { Timber.e("  $it") }
            summary.pendingScreens.forEach { Timber.d("  $it") }
            
            if (failedCount == 0) {
                Result.success(summary)
            } else {
                Result.failure(Exception("Some screens failed to refresh: $failedCount errors"))
            }
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Critical error in home screen refresh")
            Result.failure(e)
        }
    }
    
    /**
     * Check data availability for all screens
     */
    suspend fun checkAllScreensDataAvailability(
        context: Context,
        year: String
    ): DataAvailabilitySummary = withContext(Dispatchers.IO) {
        try {
            val summary = DataAvailabilitySummary()
            
            // Check Outstanding data
            if (ApiDataManager.isDataAvailable(context, "outstanding", year)) {
                summary.availableScreens.add("Outstanding")
            } else {
                summary.unavailableScreens.add("Outstanding")
            }
            
            // Check Stock data
            if (ApiDataManager.isDataAvailable(context, "stock", year)) {
                summary.availableScreens.add("Stock")
            } else {
                summary.unavailableScreens.add("Stock")
            }
            
            // TODO: Add checks for other 6 screens when implemented
            summary.pendingScreens.addAll(listOf(
                "Account Master", "Ledger", "Sale/Purchase", 
                "Expiry", "Templates", "Price Data"
            ))
            
            summary
        } catch (e: Exception) {
            Timber.e(e, "❌ Error checking data availability")
            DataAvailabilitySummary()
        }
    }
}

/**
 * Data classes for refresh results
 */
data class RefreshSummary(
    val successfulScreens: MutableList<String> = mutableListOf(),
    val failedScreens: MutableList<String> = mutableListOf(),
    val pendingScreens: MutableList<String> = mutableListOf(),
    var overallMessage: String = ""
)

data class DataAvailabilitySummary(
    val availableScreens: MutableList<String> = mutableListOf(),
    val unavailableScreens: MutableList<String> = mutableListOf(),
    val pendingScreens: MutableList<String> = mutableListOf()
)
