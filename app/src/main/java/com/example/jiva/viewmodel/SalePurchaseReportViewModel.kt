package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.SalePurchaseEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Sale/Purchase Report Screen
 * Handles data operations and business logic for sale/purchase entries
 */
class SalePurchaseReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {

    /**
     * Observe sale/purchase entries for a specific year
     */
    fun observeSalePurchase(year: String): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getByYear(year)
    }

    /**
     * Get all sale/purchase entries
     */
    fun getAllSalePurchaseEntries(): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getAllSalePurchases()
    }

    /**
     * Get sale/purchase entries by party name
     */
    fun getSalePurchaseEntriesByParty(partyName: String): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getSalePurchasesByParty(partyName)
    }

    /**
     * Get sale/purchase entries by transaction type
     */
    fun getSalePurchaseEntriesByType(trType: String): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getSalePurchasesByType(trType)
    }

    /**
     * Get sale/purchase entries by category
     */
    fun getSalePurchaseEntriesByCategory(category: String): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getSalePurchasesByCategory(category)
    }

    /**
     * Get all unique party names for filtering
     */
    suspend fun getAllPartyNames(): List<String> {
        return try {
            database.salePurchaseDao().getAllPartyNames()
        } catch (e: Exception) {
            Timber.e(e, "Error getting party names")
            emptyList()
        }
    }

    /**
     * Get all unique item names for filtering
     */
    suspend fun getAllItemNames(): List<String> {
        return try {
            database.salePurchaseDao().getAllItemNames()
        } catch (e: Exception) {
            Timber.e(e, "Error getting item names")
            emptyList()
        }
    }

    /**
     * Get all unique categories for filtering
     */
    suspend fun getAllCategories(): List<String> {
        return try {
            database.salePurchaseDao().getAllCategories()
        } catch (e: Exception) {
            Timber.e(e, "Error getting categories")
            emptyList()
        }
    }

    /**
     * Insert sale/purchase entries
     */
    fun insertSalePurchaseEntries(entries: List<SalePurchaseEntity>) {
        viewModelScope.launch {
            try {
                database.salePurchaseDao().insertSalePurchases(entries)
                Timber.d("Successfully inserted ${entries.size} sale/purchase entries")
            } catch (e: Exception) {
                Timber.e(e, "Error inserting sale/purchase entries")
            }
        }
    }

    /**
     * Clear all sale/purchase data for a specific year
     */
    fun clearSalePurchaseDataForYear(year: String) {
        viewModelScope.launch {
            try {
                database.salePurchaseDao().deleteByYear(year)
                Timber.d("Successfully cleared sale/purchase data for year: $year")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing sale/purchase data for year: $year")
            }
        }
    }

    /**
     * Factory for creating SalePurchaseReportViewModel instances
     */
    class Factory(
        private val database: JivaDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SalePurchaseReportViewModel::class.java)) {
                return SalePurchaseReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
