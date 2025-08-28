package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.PriceDataEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for PriceList Report Screen
 * Handles data operations and business logic for price list entries
 */
class PriceListReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {

    /**
     * Observe price list entries for a specific year
     */
    fun observePriceList(year: String): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getByYear(year)
    }

    /**
     * Get all price list entries
     */
    fun getAllPriceListEntries(): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getAllPriceData()
    }

    /**
     * Get price list entries sorted by item name
     */
    fun getPriceListSortedByName(): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getAllPriceDataSortedByName()
    }

    /**
     * Get price list entries sorted by MRP
     */
    fun getPriceListSortedByMrp(): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getAllPriceDataSortedByMrp()
    }

    /**
     * Get price list entries sorted by credit sale rate
     */
    fun getPriceListSortedByCreditRate(): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getAllPriceDataSortedByCreditRate()
    }

    /**
     * Search price list entries by item name
     */
    fun searchPriceListByItemName(searchTerm: String): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().searchPriceDataByItemName(searchTerm)
    }

    /**
     * Get all unique item names for filtering
     */
    suspend fun getAllItemNames(): List<String> {
        return try {
            database.priceDataDao().getAllItemNames()
        } catch (e: Exception) {
            Timber.e(e, "Error getting item names")
            emptyList()
        }
    }

    /**
     * Get price data by item ID
     */
    suspend fun getPriceDataByItemId(itemId: String): PriceDataEntity? {
        return try {
            database.priceDataDao().getPriceDataByItemId(itemId)
        } catch (e: Exception) {
            Timber.e(e, "Error getting price data for item: $itemId")
            null
        }
    }

    /**
     * Insert price list entries
     */
    fun insertPriceListEntries(entries: List<PriceDataEntity>) {
        viewModelScope.launch {
            try {
                database.priceDataDao().insertAllPriceData(entries)
                Timber.d("Successfully inserted ${entries.size} price list entries")
            } catch (e: Exception) {
                Timber.e(e, "Error inserting price list entries")
            }
        }
    }

    /**
     * Update price data
     */
    fun updatePriceData(priceData: PriceDataEntity) {
        viewModelScope.launch {
            try {
                database.priceDataDao().updatePriceData(priceData)
                Timber.d("Successfully updated price data for item: ${priceData.itemId}")
            } catch (e: Exception) {
                Timber.e(e, "Error updating price data for item: ${priceData.itemId}")
            }
        }
    }

    /**
     * Clear all price list data for a specific year
     */
    fun clearPriceListDataForYear(year: String) {
        viewModelScope.launch {
            try {
                database.priceDataDao().deleteByYear(year)
                Timber.d("Successfully cleared price list data for year: $year")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing price list data for year: $year")
            }
        }
    }

    /**
     * Factory for creating PriceListReportViewModel instances
     */
    class Factory(
        private val database: JivaDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PriceListReportViewModel::class.java)) {
                return PriceListReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
