package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * App-Level Data Loader
 * Automatically loads data from permanent storage to all screens when app opens
 * This ensures all screens have data available immediately without manual refresh
 */
object AppDataLoader {
    
    /**
     * Load all data from permanent storage to Room DB for all screens
     * Called automatically when app starts
     */
    suspend fun loadAllDataFromPermanentStorage(
        context: Context,
        database: JivaDatabase,
        year: String
    ): LoadingSummary = withContext(Dispatchers.IO) {
        try {
            Timber.d("🚀 APP STARTUP: Loading all data from permanent storage for year: $year")
            
            val summary = LoadingSummary()
            
            // 1. Load Outstanding Data
            try {
                val outstandingData = PermanentStorageManager.loadOutstandingData(context, year)
                if (outstandingData.isNotEmpty()) {
                    database.outstandingDao().clearYear(year)
                    database.outstandingDao().insertAll(outstandingData)
                    summary.loadedScreens.add("Outstanding: ${outstandingData.size} entries")
                    Timber.d("✅ Outstanding data loaded: ${outstandingData.size} entries")
                } else {
                    summary.emptyScreens.add("Outstanding: No data")
                    Timber.d("📁 Outstanding: No permanent data found")
                }
            } catch (e: Exception) {
                summary.errorScreens.add("Outstanding: ${e.message}")
                Timber.e(e, "❌ Error loading Outstanding data")
            }
            
            // 2. Load Stock Data
            try {
                val stockData = PermanentStorageManager.loadStockData(context, year)
                if (stockData.isNotEmpty()) {
                    database.stockDao().clearYear(year)
                    database.stockDao().insertAll(stockData)
                    summary.loadedScreens.add("Stock: ${stockData.size} entries")
                    Timber.d("✅ Stock data loaded: ${stockData.size} entries")
                } else {
                    summary.emptyScreens.add("Stock: No data")
                    Timber.d("📁 Stock: No permanent data found")
                }
            } catch (e: Exception) {
                summary.errorScreens.add("Stock: ${e.message}")
                Timber.e(e, "❌ Error loading Stock data")
            }
            
            // 3. TODO: Load Account Master Data
            try {
                // TODO: Implement when AccountMaster permanent storage is ready
                summary.pendingScreens.add("Account Master: Not implemented")
                Timber.d("⏳ Account Master loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Account Master: ${e.message}")
            }
            
            // 4. TODO: Load Ledger Data
            try {
                // TODO: Implement when Ledger permanent storage is ready
                summary.pendingScreens.add("Ledger: Not implemented")
                Timber.d("⏳ Ledger loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Ledger: ${e.message}")
            }
            
            // 5. TODO: Load Sale/Purchase Data
            try {
                // TODO: Implement when Sale/Purchase permanent storage is ready
                summary.pendingScreens.add("Sale/Purchase: Not implemented")
                Timber.d("⏳ Sale/Purchase loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Sale/Purchase: ${e.message}")
            }
            
            // 6. TODO: Load Expiry Data
            try {
                // TODO: Implement when Expiry permanent storage is ready
                summary.pendingScreens.add("Expiry: Not implemented")
                Timber.d("⏳ Expiry loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Expiry: ${e.message}")
            }
            
            // 7. TODO: Load Templates Data
            try {
                // TODO: Implement when Templates permanent storage is ready
                summary.pendingScreens.add("Templates: Not implemented")
                Timber.d("⏳ Templates loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Templates: ${e.message}")
            }
            
            // 8. TODO: Load Price Data
            try {
                // TODO: Implement when Price Data permanent storage is ready
                summary.pendingScreens.add("Price Data: Not implemented")
                Timber.d("⏳ Price Data loading not implemented yet")
            } catch (e: Exception) {
                summary.errorScreens.add("Price Data: ${e.message}")
            }
            
            // Generate summary
            val totalLoaded = summary.loadedScreens.size
            val totalEmpty = summary.emptyScreens.size
            val totalErrors = summary.errorScreens.size
            val totalPending = summary.pendingScreens.size
            
            summary.overallMessage = "📊 App Startup Loading: ${totalLoaded}✅ ${totalEmpty}📁 ${totalErrors}❌ ${totalPending}⏳"

            Timber.d(summary.overallMessage)
            summary.loadedScreens.forEach { Timber.d("  ✅ $it") }
            summary.emptyScreens.forEach { Timber.d("  📁 $it") }
            summary.errorScreens.forEach { Timber.e("  ❌ $it") }
            summary.pendingScreens.forEach { Timber.d("  ⏳ $it") }

            // If no data was loaded, add simple test data for development
            if (totalLoaded == 0 && totalErrors == 0) {
                try {
                    Timber.d("🔧 No permanent data found - adding simple test data for development")
                    val testDataAdded = SimpleTestDataProvider.populateIfEmpty(context, database, year)
                    if (testDataAdded) {
                        summary.loadedScreens.add("Test Data: Added for development")
                        summary.overallMessage = "🔧 Development: Simple test data added"
                    }
                } catch (e: Exception) {
                    Timber.e(e, "❌ Failed to add test data")
                }
            }

            summary
            
        } catch (e: Exception) {
            Timber.e(e, "❌ Critical error in app startup data loading")
            LoadingSummary().apply {
                errorScreens.add("Critical error: ${e.message}")
                overallMessage = "❌ App startup loading failed"
            }
        }
    }
    
    /**
     * Check if any data is available in permanent storage
     */
    suspend fun hasAnyDataInPermanentStorage(
        context: Context,
        year: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val hasOutstanding = PermanentStorageManager.isDataAvailable(context, "outstanding", year)
            val hasStock = PermanentStorageManager.isDataAvailable(context, "stock", year)
            
            val hasAnyData = hasOutstanding || hasStock
            Timber.d("📊 Data availability check: Outstanding=$hasOutstanding, Stock=$hasStock, HasAny=$hasAnyData")
            
            hasAnyData
        } catch (e: Exception) {
            Timber.e(e, "❌ Error checking data availability")
            false
        }
    }
    
    /**
     * Get data summary for debugging
     */
    suspend fun getDataSummary(
        context: Context,
        year: String
    ): String = withContext(Dispatchers.IO) {
        try {
            val outstandingSize = PermanentStorageManager.getDataFileSize(context, "outstanding", year)
            val stockSize = PermanentStorageManager.getDataFileSize(context, "stock", year)
            
            buildString {
                appendLine("📊 Permanent Storage Summary:")
                appendLine("  Outstanding: ${outstandingSize}KB")
                appendLine("  Stock: ${stockSize}KB")
                appendLine("  Year: $year")
            }
        } catch (e: Exception) {
            "❌ Error getting data summary: ${e.message}"
        }
    }
}

/**
 * Data class for loading results
 */
data class LoadingSummary(
    val loadedScreens: MutableList<String> = mutableListOf(),
    val emptyScreens: MutableList<String> = mutableListOf(),
    val errorScreens: MutableList<String> = mutableListOf(),
    val pendingScreens: MutableList<String> = mutableListOf(),
    var overallMessage: String = ""
)
