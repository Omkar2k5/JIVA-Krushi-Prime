package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.LedgerEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Ledger Report Screen
 * Handles data operations and business logic for ledger entries
 */
class LedgerReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {

    // Remote data source for Account_Names API
    private val remote = com.example.jiva.data.network.RemoteDataSource()

    // Expose account options (id + name) for dropdown
    data class AccountOption(val id: String, val name: String)

    private val _accountOptions = MutableStateFlow<List<AccountOption>>(emptyList())
    val accountOptions: StateFlow<List<AccountOption>> = _accountOptions.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /**
     * Load account names from API and expose as options
     */
    fun loadAccountNames(userId: Int, year: String) {
        viewModelScope.launch {
            try {
                val res = remote.getAccountNames(userId, year)
                if (res.isSuccess) {
                    val data = res.getOrNull()?.data.orEmpty()
                    _accountOptions.value = data.mapNotNull { item ->
                        val name = (item.accountName ?: item.items)?.trim()
                        val id = item.acId?.trim()
                        if (!name.isNullOrBlank() && !id.isNullOrBlank()) AccountOption(id = id, name = name) else null
                    }.distinctBy { it.id }.sortedBy { it.name }
                    _error.value = null
                } else {
                    _error.value = res.exceptionOrNull()?.message
                    _accountOptions.value = emptyList()
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account names")
                _error.value = e.message
                _accountOptions.value = emptyList()
            }
        }
    }

    /**
     * Observe ledger entries for a specific year
     */
    fun observeLedger(year: String): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByYear(year)
    }

    /**
     * Get all ledger entries
     */
    fun getAllLedgerEntries(): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getAllLedgerEntries()
    }

    /**
     * Get ledger entries by account ID
     */
    fun getLedgerEntriesByAccount(acId: Int): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByAccount(acId)
    }

    /**
     * Get ledger entries by entry type
     */
    fun getLedgerEntriesByType(entryType: String): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByType(entryType)
    }

    /**
     * Get ledger entries by company code
     */
    fun getLedgerEntriesByCompany(cmpCode: Int): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByCompany(cmpCode)
    }

    /**
     * Get all unique entry types for filtering
     */
    suspend fun getAllEntryTypes(): List<String> {
        return try {
            database.ledgerDao().getAllEntryTypes()
        } catch (e: Exception) {
            Timber.e(e, "Error getting entry types")
            emptyList()
        }
    }

    /**
     * Insert ledger entries
     */
    fun insertLedgerEntries(entries: List<LedgerEntity>) {
        viewModelScope.launch {
            try {
                database.ledgerDao().insertLedgerEntries(entries)
                Timber.d("Successfully inserted ${entries.size} ledger entries")
            } catch (e: Exception) {
                Timber.e(e, "Error inserting ledger entries")
            }
        }
    }

    /**
     * Clear all ledger data for a specific year
     */
    fun clearLedgerDataForYear(year: String) {
        viewModelScope.launch {
            try {
                database.ledgerDao().deleteByYear(year)
                Timber.d("Successfully cleared ledger data for year: $year")
            } catch (e: Exception) {
                Timber.e(e, "Error clearing ledger data for year: $year")
            }
        }
    }

    /**
     * Factory for creating LedgerReportViewModel instances
     */
    class Factory(
        private val database: JivaDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LedgerReportViewModel::class.java)) {
                return LedgerReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
