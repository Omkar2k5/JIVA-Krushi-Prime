package com.example.jiva.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Permanent Storage Manager for all app data
 * Handles persistent storage in Android internal storage
 * All screens read from this permanent storage only
 */
object PermanentStorageManager {
    
    private const val OUTSTANDING_FILE = "outstanding_permanent.json"
    private const val STOCK_FILE = "stock_permanent.json"
    private const val ACCOUNTS_FILE = "accounts_permanent.json"
    private const val LEDGER_FILE = "ledger_permanent.json"
    private const val SALE_PURCHASE_FILE = "sale_purchase_permanent.json"
    private const val EXPIRY_FILE = "expiry_permanent.json"
    private const val TEMPLATES_FILE = "templates_permanent.json"
    private const val PRICE_DATA_FILE = "price_data_permanent.json"
    
    private val gson = Gson()
    
    /**
     * Save Outstanding data to permanent storage
     */
    suspend fun saveOutstandingData(
        context: Context,
        data: List<com.example.jiva.data.database.entities.OutstandingEntity>,
        year: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$OUTSTANDING_FILE"
            val file = File(context.filesDir, fileName)
            
            val jsonData = gson.toJson(data)
            FileWriter(file).use { writer ->
                writer.write(jsonData)
            }
            
            Timber.d("✅ Saved ${data.size} outstanding entries to permanent storage: $fileName")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save outstanding data to permanent storage")
            false
        }
    }
    
    /**
     * Load Outstanding data from permanent storage
     */
    suspend fun loadOutstandingData(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.OutstandingEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$OUTSTANDING_FILE"
            val file = File(context.filesDir, fileName)
            
            if (!file.exists()) {
                Timber.d("📁 Outstanding data file does not exist: $fileName")
                return@withContext emptyList()
            }
            
            val jsonData = FileReader(file).use { reader ->
                reader.readText()
            }
            
            val type = object : TypeToken<List<com.example.jiva.data.database.entities.OutstandingEntity>>() {}.type
            val data: List<com.example.jiva.data.database.entities.OutstandingEntity> = gson.fromJson(jsonData, type)
            
            Timber.d("✅ Loaded ${data.size} outstanding entries from permanent storage: $fileName")
            data
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load outstanding data from permanent storage")
            emptyList()
        }
    }
    
    /**
     * Save Stock data to permanent storage
     */
    suspend fun saveStockData(
        context: Context,
        data: List<com.example.jiva.data.database.entities.StockEntity>,
        year: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$STOCK_FILE"
            val file = File(context.filesDir, fileName)
            
            val jsonData = gson.toJson(data)
            FileWriter(file).use { writer ->
                writer.write(jsonData)
            }
            
            Timber.d("✅ Saved ${data.size} stock entries to permanent storage: $fileName")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to save stock data to permanent storage")
            false
        }
    }
    
    /**
     * Load Stock data from permanent storage
     */
    suspend fun loadStockData(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.StockEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$STOCK_FILE"
            val file = File(context.filesDir, fileName)
            
            if (!file.exists()) {
                Timber.d("📁 Stock data file does not exist: $fileName")
                return@withContext emptyList()
            }
            
            val jsonData = FileReader(file).use { reader ->
                reader.readText()
            }
            
            val type = object : TypeToken<List<com.example.jiva.data.database.entities.StockEntity>>() {}.type
            val data: List<com.example.jiva.data.database.entities.StockEntity> = gson.fromJson(jsonData, type)
            
            Timber.d("✅ Loaded ${data.size} stock entries from permanent storage: $fileName")
            data
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to load stock data from permanent storage")
            emptyList()
        }
    }
    
    /**
     * Check if data exists in permanent storage
     */
    suspend fun isDataAvailable(
        context: Context,
        dataType: String,
        year: String = ""
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = when (dataType) {
                "outstanding" -> "${year}_$OUTSTANDING_FILE"
                "stock" -> "${year}_$STOCK_FILE"
                "accounts" -> ACCOUNTS_FILE
                "ledger" -> LEDGER_FILE
                "sale_purchase" -> SALE_PURCHASE_FILE
                "expiry" -> EXPIRY_FILE
                "templates" -> TEMPLATES_FILE
                "price_data" -> PRICE_DATA_FILE
                else -> return@withContext false
            }
            
            val file = File(context.filesDir, fileName)
            val exists = file.exists() && file.length() > 0
            
            Timber.d("📁 Data availability check - $dataType: $exists")
            exists
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to check data availability for $dataType")
            false
        }
    }
    
    /**
     * Get file size for monitoring
     */
    suspend fun getDataFileSize(
        context: Context,
        dataType: String,
        year: String = ""
    ): Long = withContext(Dispatchers.IO) {
        try {
            val fileName = when (dataType) {
                "outstanding" -> "${year}_$OUTSTANDING_FILE"
                "stock" -> "${year}_$STOCK_FILE"
                else -> return@withContext 0L
            }
            
            val file = File(context.filesDir, fileName)
            if (file.exists()) {
                file.length() / 1024 // Size in KB
            } else {
                0L
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to get file size for $dataType")
            0L
        }
    }
    
    /**
     * Clear old data files (cleanup)
     */
    suspend fun clearOldData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val filesDir = context.filesDir
            val dataFiles = filesDir.listFiles { _, name ->
                name.endsWith("_permanent.json")
            }
            
            var deletedCount = 0
            dataFiles?.forEach { file ->
                val fileAge = System.currentTimeMillis() - file.lastModified()
                val maxAgeMillis = 30 * 24 * 60 * 60 * 1000L // 30 days
                
                if (fileAge > maxAgeMillis) {
                    if (file.delete()) {
                        deletedCount++
                        Timber.d("🗑️ Deleted old data file: ${file.name}")
                    }
                }
            }
            
            Timber.d("🧹 Cleaned up $deletedCount old data files")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to clear old data")
            false
        }
    }

    /**
     * Delete ALL Outstanding permanent storage files across all years
     */
    suspend fun deleteAllOutstandingPermanentData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val filesDir = context.filesDir
            val dataFiles = filesDir.listFiles { _, name ->
                name.endsWith("_outstanding_permanent.json")
            }

            var deletedCount = 0
            dataFiles?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                    Timber.d("🗑️ Deleted Outstanding permanent file: ${file.name}")
                }
            }

            Timber.d("✅ Deleted $deletedCount Outstanding permanent files")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to delete Outstanding permanent files")
            false
        }
    }

    /**
     * Delete ALL Ledger permanent storage files across all years
     */
    suspend fun deleteAllLedgerPermanentData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val filesDir = context.filesDir
            val dataFiles = filesDir.listFiles { _, name ->
                name.endsWith("_ledger_permanent.json")
            }

            var deletedCount = 0
            dataFiles?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                    Timber.d("🗑️ Deleted Ledger permanent file: ${file.name}")
                }
            }

            Timber.d("✅ Deleted $deletedCount Ledger permanent files")
            true
        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to delete Ledger permanent files")
            false
        }
    }
}
