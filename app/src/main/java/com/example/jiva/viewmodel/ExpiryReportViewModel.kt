package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.ExpiryEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Expiry Report Screen
 * Handles data operations and business logic for expiry entries
 */
class ExpiryReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {

    /**
     * Observe expiry entries for a specific year
     */
    fun observeExpiry(year: String): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getByYear(year)
    }

    /**
     * Get all expiry entries
     */
    fun getAllExpiryEntries(): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getAllExpiryItems()
    }

    /**
     * Get expiry entries by item type
     */
    fun getExpiryEntriesByType(itemType: String): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getExpiryItemsByType(itemType)
    }

    /**
     * Get expiry entries by item name
     */
    fun getExpiryEntriesByItemName(itemName: String): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getExpiryItemsByItemName(itemName)
    }

    /**
     * Get expiry entries by batch number
     */
    fun getExpiryEntriesByBatch(batchNo: String): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getExpiryItemsByBatch(batchNo)
    }

    /**
     * Get expired items (days left <= 0)
     */
    fun getExpiredItems(): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getExpiredItems()
    }

    /**
     * Get items expiring soon (days left <= specified days)
     */
    fun getItemsExpiringSoon(days: Int): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getItemsExpiringSoon(days)
    }

    /**
     * Get all unique item types for filtering
     */
    suspend fun getAllItemTypes(): List<String> {
        return try {
            database.expiryDao().getAllItemTypes()
        } catch (e: Exception) {
            Timber.e(e, "Error getting item types")
            emptyList()
        }
    }

    /**
     * Get all unique item names for filtering
     */
    suspend fun getAllItemNames(): List<String> {
        return try {
            database.expiryDao().getAllItemNames()
        } catch (e: Exception) {
            Timber.e(e, "Error getting item names")
            emptyList()
        }
    }

    /**
     * Get all unique batch numbers for filtering
     */
    suspend fun getAllBatchNumbers(): List<String> {
        return try {
            database.expiryDao().getAllBatchNumbers()
        } catch (e: Exception) {
            Timber.e(e, "Error getting batch numbers")
            emptyList()
        }
    }

    /**
     * Insert expiry entries
     */
    fun insertExpiryEntries(entries: List<ExpiryEntity>) {
        viewModelScope.launch {
            try {
                database.expiryDao().insertExpiryItems(entries)
                Timber.d("Successfully inserted ${entries.size} expiry entries")
            } catch (e: Exception) {
                Timber.e(e, "Error inserting expiry entries")
            }
        }
    }

    /**
     * Clear all expiry data for a specific year
     */
    fun clearExpiryDataForYear(year: String) {
        viewModelScope.launch {
            try {
                database.expiryDao().deleteByYear(year)
                Timber.d("Successfully cleared expiry data for year: $year")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing expiry data for year: $year")
            }
        }
    }

    /**
     * Factory for creating ExpiryReportViewModel instances
     */
    class Factory(
        private val database: JivaDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpiryReportViewModel::class.java)) {
                return ExpiryReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
