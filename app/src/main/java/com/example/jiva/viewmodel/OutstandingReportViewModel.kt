package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.AccountMasterEntity
import com.example.jiva.data.database.entities.ClosingBalanceEntity
import com.example.jiva.data.repository.JivaRepository
import com.example.jiva.data.repository.JivaRepositoryImpl
import com.example.jiva.screens.OutstandingEntry
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Outstanding Report Screen
 * Uses dummy data from the repository
 */
class OutstandingReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {
    
    // Create repository instance
    private val repository: JivaRepository = JivaRepositoryImpl(database)
    
    private val _uiState = MutableStateFlow(OutstandingReportUiState())
    val uiState: StateFlow<OutstandingReportUiState> = _uiState.asStateFlow()
    
    init {
        loadOutstandingData()
        syncDataFromServer()
    }

    // Observe Outstanding table directly (fast for large datasets)
    fun observeOutstanding(year: String): Flow<List<com.example.jiva.data.database.entities.OutstandingEntity>> {
        return repository.getOutstandingFlow(year)
    }

    // Trigger Outstanding sync with explicit params
    fun syncOutstanding(userId: Int, year: String) {
        viewModelScope.launch {
            try {
                repository.syncOutstanding(userId, year)
            } catch (e: Exception) {
                Timber.e(e, "Outstanding sync failed")
            }
        }
    }
    
    private fun loadOutstandingData() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Combine account master and closing balance data
                repository.getAllAccounts()
                    .combine(repository.getAllClosingBalances()) { accounts, balances ->
                        accounts.map { account ->
                            val closingBalance = balances.find { it.acId == account.acId }
                            toOutstandingEntry(account, closingBalance)
                        }
                    }
                    .collect { outstandingEntries ->
                        _uiState.value = _uiState.value.copy(
                            outstandingEntries = outstandingEntries,
                            isLoading = false,
                            error = null
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error loading outstanding data")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    private fun syncDataFromServer() {
        viewModelScope.launch {
            try {
                repository.syncAccounts()
                repository.syncClosingBalances()
                Timber.d("Successfully loaded dummy data")
            } catch (e: Exception) {
                Timber.e(e, "Error loading dummy data")
            }
        }
    }
    
    fun searchByName(searchTerm: String) {
        viewModelScope.launch {
            try {
                repository.searchAccountsByName(searchTerm)
                    .combine(repository.getAllClosingBalances()) { accounts, balances ->
                        accounts.map { account ->
                            val closingBalance = balances.find { it.acId == account.acId }
                            toOutstandingEntry(account, closingBalance)
                        }
                    }
                    .collect { filteredEntries ->
                        _uiState.value = _uiState.value.copy(
                            outstandingEntries = filteredEntries
                        )
                    }
            } catch (e: Exception) {
                Timber.e(e, "Error searching accounts")
            }
        }
    }
    
    fun filterByArea(area: String) {
        // Implementation for area filtering
        // You can add this to the DAO queries
    }
    
    // Helper function to convert database entities to UI model
    private fun toOutstandingEntry(
        account: AccountMasterEntity,
        closingBalance: ClosingBalanceEntity?
    ): OutstandingEntry {
        return OutstandingEntry(
            acId = account.acId.toString(),
            accountName = account.accountName,
            mobile = account.mobile ?: "",
            under = account.area ?: "",
            balance = (closingBalance?.balance?.toString() ?: account.openingBalance.toString()),
            lastDate = "", // Would need transaction data to calculate this
            days = "", // Would need transaction data to calculate this
            creditLimitAmount = "", // Would need credit limit data
            creditLimitDays = "" // Would need credit limit data
        )
    }
    
    /**
     * Factory for creating OutstandingReportViewModel with dependencies
     */
    class Factory(private val database: JivaDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(OutstandingReportViewModel::class.java)) {
                return OutstandingReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

data class OutstandingReportUiState(
    val outstandingEntries: List<OutstandingEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
