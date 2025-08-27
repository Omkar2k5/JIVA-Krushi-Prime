package com.example.jiva.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.StockEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * ViewModel for Stock Report Screen
 */
class StockReportViewModel(
    private val database: JivaDatabase
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val error: String? = null,
        val stockEntries: List<StockEntry> = emptyList()
    )

    // Legacy data model for compatibility
    data class StockEntry(
        val itemId: String,
        val itemName: String,
        val opening: String,
        val inWard: String,
        val outWard: String,
        val closingStock: String,
        val avgRate: String,
        val valuation: String,
        val itemType: String,
        val company: String,
        val cgst: String,
        val sgst: String,
        val igst: String
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Removed automatic data loading from init for better performance
    // Data will be loaded from Room DB only, API calls only on manual refresh

    // Observe Stock data from Room DB
    fun observeStock(year: String): Flow<List<StockEntity>> {
        return database.stockDao().getAll(year)
    }

    // Trigger Stock sync with explicit params and local storage
    fun syncStock(userId: Int, year: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                Timber.d("Starting Stock sync for userId: $userId, year: $year")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                // First, try to load from local storage
                val localData = com.example.jiva.utils.LocalDataStorage.loadStockData(context, year)
                if (localData.isNotEmpty()) {
                    Timber.d("Loading ${localData.size} entries from local storage")
                    // Update Room DB with local data
                    database.stockDao().clearYear(year)
                    database.stockDao().insertAll(localData)
                }
                
                // Then sync from API and update local storage
                val repository = com.example.jiva.data.repository.JivaRepositoryImpl(database, 
                    com.example.jiva.data.network.RemoteDataSource(
                        com.example.jiva.data.network.RetrofitClient.jivaApiService
                    )
                )
                val result = repository.syncStock(userId, year)
                if (result.isSuccess) {
                    Timber.d("Stock sync completed successfully")
                    
                    // Save updated data to local storage
                    val updatedData = database.stockDao().getAllSync(year)
                    com.example.jiva.utils.LocalDataStorage.saveStockData(context, updatedData, year)
                    
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Stock sync failed"
                    Timber.e("Stock sync failed: $errorMsg")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                Timber.e(e, "Stock sync failed with exception")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
    
    // Load data from local storage on startup
    fun loadFromLocalStorage(context: android.content.Context, year: String) {
        viewModelScope.launch {
            try {
                Timber.d("Loading Stock data from local storage for year: $year")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)
                
                val localData = com.example.jiva.utils.LocalDataStorage.loadStockData(context, year)
                if (localData.isNotEmpty()) {
                    Timber.d("Found ${localData.size} entries in local storage")
                    // Update Room DB with local data
                    database.stockDao().clearYear(year)
                    database.stockDao().insertAll(localData)
                    
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                } else {
                    Timber.d("No local data found, database will show empty until refresh")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load from local storage")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    // Factory for creating ViewModel with database dependency
    class Factory(private val database: JivaDatabase) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StockReportViewModel::class.java)) {
                return StockReportViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
