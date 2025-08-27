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

    // Refresh Stock data - API call → Permanent Storage
    fun refreshStockData(userId: Int, year: String, context: android.content.Context) {
        viewModelScope.launch {
            try {
                Timber.d("🔄 Starting Stock refresh for userId: $userId, year: $year")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                // Create repository instance (RemoteDataSource has no-arg constructor)
                val repository = com.example.jiva.data.repository.JivaRepositoryImpl(database)

                // Use ApiDataManager to handle API → Permanent Storage
                val result = com.example.jiva.utils.ApiDataManager.refreshStockData(
                    context = context,
                    repository = repository,
                    database = database,
                    userId = userId,
                    year = year
                )

                if (result.isSuccess) {
                    Timber.d("✅ Stock refresh completed successfully")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Stock refresh failed"
                    Timber.e("❌ Stock refresh failed: $errorMsg")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = errorMsg)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Stock refresh failed with exception")
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }
    
    // Load data from permanent storage only (for UI display)
    fun loadFromPermanentStorage(context: android.content.Context, year: String) {
        viewModelScope.launch {
            try {
                Timber.d("📁 Loading Stock data from permanent storage for year: $year")
                _uiState.value = _uiState.value.copy(isLoading = true, error = null)

                val permanentData = com.example.jiva.utils.ApiDataManager.loadStockDataForUI(context, year)
                if (permanentData.isNotEmpty()) {
                    Timber.d("✅ Found ${permanentData.size} entries in permanent storage")
                    // Update Room DB for reactive UI
                    database.stockDao().clearYear(year)
                    database.stockDao().insertAll(permanentData)

                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                } else {
                    Timber.d("📁 No permanent data found, showing empty until refresh")
                    _uiState.value = _uiState.value.copy(isLoading = false, error = null)
                }
            } catch (e: Exception) {
                Timber.e(e, "❌ Failed to load from permanent storage")
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
