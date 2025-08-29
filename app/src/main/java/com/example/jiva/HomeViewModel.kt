package com.example.jiva

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.model.User
import com.example.jiva.data.repository.AuthRepository
import com.example.jiva.data.repository.JivaRepository
import com.example.jiva.utils.CredentialManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class HomeUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val syncSuccess: Boolean = false,
    val errorMessage: String? = null
)

class HomeViewModel(
    application: Application,
    private val authRepository: AuthRepository,
    private val jivaRepository: JivaRepository? = null
) : AndroidViewModel(application) {
    
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    fun loadUserSession() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val session = authRepository.getCurrentSession()
                if (session != null) {
                    _uiState.value = _uiState.value.copy(
                        currentUser = session.user,
                        isLoading = false,
                        errorMessage = null
                    )
                    Timber.d("User session loaded: ${session.user.username}")
                } else {
                    _uiState.value = _uiState.value.copy(
                        currentUser = null,
                        isLoading = false,
                        errorMessage = "No active session found"
                    )
                    Timber.w("No active user session found")
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading user session")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to load user session"
                )
            }
        }
    }
    
    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _uiState.value = HomeUiState() // Reset state
                Timber.d("User logged out successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Failed to logout properly"
                )
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    /**
     * Syncs all business data endpoints sequentially to avoid server/app overload.
     * Calls: Outstanding, Ledger, Stock, Sale/Purchase, Expiry, Price List.
     */
    fun syncData() {
        viewModelScope.launch {
            try {
                // Only proceed if repository is available
                if (jivaRepository == null) {
                    _uiState.value = _uiState.value.copy(
                        errorMessage = "Repository not available for sync",
                        isSyncing = false,
                        syncSuccess = false
                    )
                    return@launch
                }

                // Set syncing state
                _uiState.value = _uiState.value.copy(
                    isSyncing = true,
                    syncSuccess = false,
                    errorMessage = null
                )

                // Resolve user and year context
                val context = getApplication<JivaApplication>().applicationContext
                val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
                val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

                if (userId == null) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = false,
                        errorMessage = "Cannot sync: missing user id"
                    )
                    return@launch
                }

                // Build sequential tasks
                val tasks: List<Pair<String, suspend () -> Result<Unit>>> = listOf(
                    "Outstanding" to { jivaRepository.syncOutstanding(userId, year) },
                    "Ledger" to { jivaRepository.syncLedger(userId, year) },
                    "Stock" to { jivaRepository.syncStock(userId, year) },
                    "Sale/Purchase" to { jivaRepository.syncSalePurchase(userId, year) },
                    "Expiry" to { jivaRepository.syncExpiry(userId, year) },
                    "Price List" to { jivaRepository.syncPriceList(userId, year) }
                )

                var anySuccess = false
                val failures = mutableListOf<String>()

                for ((name, task) in tasks) {
                    try {
                        Timber.d("Starting sequential sync for $name ...")
                        val result = task()
                        if (result.isSuccess) {
                            anySuccess = true
                            Timber.d("$name sync succeeded")
                        } else {
                            val msg = result.exceptionOrNull()?.message ?: "unknown error"
                            failures.add("$name: $msg")
                            Timber.w("$name sync failed: $msg")
                        }
                    } catch (e: Exception) {
                        failures.add("$name: ${e.message ?: "exception"}")
                        Timber.w(e, "$name sync threw exception")
                    }

                    // Small gap between calls to reduce load on server
                    kotlinx.coroutines.delay(500)
                }

                // Update final state
                if (anySuccess) {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = true,
                        errorMessage = if (failures.isNotEmpty()) failures.joinToString("; ") else null
                    )
                    // Reset success state after 3 seconds
                    kotlinx.coroutines.delay(3000)
                    _uiState.value = _uiState.value.copy(syncSuccess = false, errorMessage = null)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = false,
                        errorMessage = if (failures.isNotEmpty()) failures.joinToString("; ") else "All sync calls failed"
                    )
                }
            } catch (e: Exception) {
                Timber.e(e, "Critical error during sequential data sync")
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncSuccess = false,
                    errorMessage = "Critical sync error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
}
