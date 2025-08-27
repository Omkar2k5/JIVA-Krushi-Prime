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
import java.io.IOException

/**
 * Local file storage utility for fast data access
 * Stores data in JSON format in app's internal storage
 */
object LocalDataStorage {

    private const val OUTSTANDING_FILE = "outstanding_data.json"
    private const val STOCK_FILE = "stock_data.json"
    private val gson = Gson()
    
    /**
     * Save outstanding data to local file storage
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
            
            Timber.d("Successfully saved ${data.size} outstanding entries to local storage: $fileName")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save outstanding data to local storage")
            false
        }
    }
    
    /**
     * Load outstanding data from local file storage
     */
    suspend fun loadOutstandingData(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.OutstandingEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$OUTSTANDING_FILE"
            val file = File(context.filesDir, fileName)
            
            if (!file.exists()) {
                Timber.d("Outstanding data file does not exist: $fileName")
                return@withContext emptyList()
            }
            
            val jsonData = FileReader(file).use { reader ->
                reader.readText()
            }
            
            val type = object : TypeToken<List<com.example.jiva.data.database.entities.OutstandingEntity>>() {}.type
            val data: List<com.example.jiva.data.database.entities.OutstandingEntity> = gson.fromJson(jsonData, type)
            
            Timber.d("Successfully loaded ${data.size} outstanding entries from local storage: $fileName")
            data
        } catch (e: Exception) {
            Timber.e(e, "Failed to load outstanding data from local storage")
            emptyList()
        }
    }
    
    /**
     * Check if local outstanding data exists and is recent
     */
    suspend fun isOutstandingDataAvailable(
        context: Context,
        year: String,
        maxAgeHours: Int = 24
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$OUTSTANDING_FILE"
            val file = File(context.filesDir, fileName)
            
            if (!file.exists()) {
                return@withContext false
            }
            
            val fileAge = System.currentTimeMillis() - file.lastModified()
            val maxAgeMillis = maxAgeHours * 60 * 60 * 1000L
            
            val isRecent = fileAge < maxAgeMillis
            Timber.d("Outstanding data file exists: $fileName, is recent: $isRecent (age: ${fileAge / 1000 / 60} minutes)")
            
            isRecent
        } catch (e: Exception) {
            Timber.e(e, "Failed to check outstanding data availability")
            false
        }
    }
    
    /**
     * Clear old outstanding data files
     */
    suspend fun clearOldOutstandingData(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val filesDir = context.filesDir
            val outstandingFiles = filesDir.listFiles { _, name ->
                name.endsWith(OUTSTANDING_FILE)
            }
            
            var deletedCount = 0
            outstandingFiles?.forEach { file ->
                val fileAge = System.currentTimeMillis() - file.lastModified()
                val maxAgeMillis = 7 * 24 * 60 * 60 * 1000L // 7 days
                
                if (fileAge > maxAgeMillis) {
                    if (file.delete()) {
                        deletedCount++
                        Timber.d("Deleted old outstanding data file: ${file.name}")
                    }
                }
            }
            
            Timber.d("Cleared $deletedCount old outstanding data files")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to clear old outstanding data")
            false
        }
    }
    
    /**
     * Get file size in KB for monitoring storage usage
     */
    suspend fun getOutstandingDataFileSize(
        context: Context,
        year: String
    ): Long = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$OUTSTANDING_FILE"
            val file = File(context.filesDir, fileName)

            if (file.exists()) {
                file.length() / 1024 // Size in KB
            } else {
                0L
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get outstanding data file size")
            0L
        }
    }

    // Stock Data Storage Methods

    /**
     * Save stock data to local file storage
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

            Timber.d("Successfully saved ${data.size} stock entries to local storage: $fileName")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to save stock data to local storage")
            false
        }
    }

    /**
     * Load stock data from local file storage
     */
    suspend fun loadStockData(
        context: Context,
        year: String
    ): List<com.example.jiva.data.database.entities.StockEntity> = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$STOCK_FILE"
            val file = File(context.filesDir, fileName)

            if (!file.exists()) {
                Timber.d("Stock data file does not exist: $fileName")
                return@withContext emptyList()
            }

            val jsonData = FileReader(file).use { reader ->
                reader.readText()
            }

            val type = object : TypeToken<List<com.example.jiva.data.database.entities.StockEntity>>() {}.type
            val data: List<com.example.jiva.data.database.entities.StockEntity> = gson.fromJson(jsonData, type)

            Timber.d("Successfully loaded ${data.size} stock entries from local storage: $fileName")
            data
        } catch (e: Exception) {
            Timber.e(e, "Failed to load stock data from local storage")
            emptyList()
        }
    }

    /**
     * Check if local stock data exists and is recent
     */
    suspend fun isStockDataAvailable(
        context: Context,
        year: String,
        maxAgeHours: Int = 24
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val fileName = "${year}_$STOCK_FILE"
            val file = File(context.filesDir, fileName)

            if (!file.exists()) {
                return@withContext false
            }

            val fileAge = System.currentTimeMillis() - file.lastModified()
            val maxAgeMillis = maxAgeHours * 60 * 60 * 1000L

            val isRecent = fileAge < maxAgeMillis
            Timber.d("Stock data file exists: $fileName, is recent: $isRecent (age: ${fileAge / 1000 / 60} minutes)")

            isRecent
        } catch (e: Exception) {
            Timber.e(e, "Failed to check stock data availability")
            false
        }
    }
}
