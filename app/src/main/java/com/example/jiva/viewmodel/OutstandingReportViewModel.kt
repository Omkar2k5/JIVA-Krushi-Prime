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

    // Network data source for new endpoints
    private val remote = com.example.jiva.data.network.RemoteDataSource()
    
    private val _uiState = MutableStateFlow(OutstandingReportUiState())
    val uiState: StateFlow<OutstandingReportUiState> = _uiState.asStateFlow()

    // Account names and area options for dropdowns
    private val _accountNames = MutableStateFlow<List<String>>(emptyList())
    val accountNames: StateFlow<List<String>> = _accountNames.asStateFlow()

    private val _areaOptions = MutableStateFlow<List<String>>(emptyList())
    val areaOptions: StateFlow<List<String>> = _areaOptions.asStateFlow()

    // Observe Outstanding table directly (fast for large datasets)
    fun observeOutstanding(year: String): Flow<List<com.example.jiva.data.database.entities.OutstandingEntity>> {
        return repository.getOutstandingFlow(year)
    }

    // Load account names and areas
    fun loadAccountNames(userId: Int, year: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val res = remote.getAccountNames(userId, year)
                if (res.isSuccess) {
                    val data = res.getOrNull()?.data.orEmpty()
                    // Sanitize possible nulls from API; prefer 'account_Name' then 'items'
                    _accountNames.value = data
                        .mapNotNull { (it.accountName ?: it.items)?.takeIf { a -> a.isNotBlank() } }
                        .distinct()
                        .sorted()
                    _areaOptions.value = data
                        .mapNotNull { it.area?.takeIf { a -> a.isNotBlank() } }
                        .distinct()
                        .sorted()
                    _uiState.value = _uiState.value.copy(isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load account names")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Fetch filtered Outstanding entries (no Room write, UI only)
    fun fetchOutstandingFiltered(userId: Int, year: String, accountName: String?, area: String?, under: String?) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                val filters = mutableMapOf<String, String>()
                if (!accountName.isNullOrBlank()) filters["account_Name"] = accountName
                if (!area.isNullOrBlank()) filters["area"] = area
                if (!under.isNullOrBlank()) filters["under"] = under

                val res = remote.getOutstanding(userId, year, if (filters.isEmpty()) null else filters)
                if (res.isSuccess) {
                    val entries = res.getOrNull()?.data.orEmpty().map {
                        OutstandingEntry(
                            acId = it.acId ?: "",
                            accountName = it.accountName ?: "",
                            mobile = it.mobile ?: "",
                            under = it.under ?: "",
                            area = it.area ?: "",
                            balance = it.balance ?: "0",
                            lastDate = it.lastDate ?: "",
                            days = it.days ?: "",
                            creditLimitAmount = it.creditLimitAmount ?: "",
                            creditLimitDays = it.creditLimitDays ?: ""
                        )
                    }
                    _uiState.value = _uiState.value.copy(outstandingEntries = entries, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = res.exceptionOrNull()?.message)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to fetch filtered outstanding")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Note: Data loading is now handled automatically by AppDataLoader
    // This ViewModel only handles refresh (API calls) and observes Room DB
    // No manual loading needed - data is automatically available from app startup
    
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
            under = account.under ?: "",
            area = account.area ?: "",
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
