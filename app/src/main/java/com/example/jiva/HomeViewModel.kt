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
     * Syncs all data from the server
     * Updates the UI state with sync status
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
                
                try {
                    // Perform sync operation
                    val result = jivaRepository.syncAllData()

                    // Also sync Outstanding data with user context if available
                    try {
                        val context = getApplication<JivaApplication>().applicationContext
                        val userId = com.example.jiva.utils.UserEnv.getUserId(context)?.toIntOrNull()
                        val year = com.example.jiva.utils.UserEnv.getFinancialYear(context) ?: "2025-26"

                        if (userId != null) {
                            val outstandingResult = jivaRepository.syncOutstanding(userId, year)
                            if (outstandingResult.isSuccess) {
                                Timber.d("Successfully synced Outstanding data for user $userId, year $year")
                            } else {
                                Timber.w("Failed to sync Outstanding data: ${outstandingResult.exceptionOrNull()?.message}")
                            }
                        } else {
                            Timber.w("No user ID available for Outstanding sync")
                        }
                    } catch (e: Exception) {
                        Timber.w(e, "Error during Outstanding sync in main refresh")
                    }

                    if (result.isSuccess) {
                        _uiState.value = _uiState.value.copy(
                            isSyncing = false,
                            syncSuccess = true,
                            errorMessage = null
                        )
                        Timber.d("Data sync completed successfully")
                        
                        // Reset success state after 3 seconds
                        kotlinx.coroutines.delay(3000)
                        _uiState.value = _uiState.value.copy(syncSuccess = false)
                    } else {
                        // Even if API call failed, we still loaded dummy data as fallback
                        // So we'll show a warning but still mark as success
                        val error = result.exceptionOrNull()?.message ?: "Unknown error during sync"
                        
                        if (error.contains("Server not available") || error.contains("using dummy data")) {
                            // Server not available, but we loaded dummy data
                            _uiState.value = _uiState.value.copy(
                                isSyncing = false,
                                syncSuccess = true,
                                errorMessage = "Server not available - using local data"
                            )
                            Timber.w("Server not available, using dummy data")
                            
                            // Reset success state after 3 seconds
                            kotlinx.coroutines.delay(3000)
                            _uiState.value = _uiState.value.copy(
                                syncSuccess = false,
                                errorMessage = null
                            )
                        } else {
                            // Other error
                            _uiState.value = _uiState.value.copy(
                                isSyncing = false,
                                syncSuccess = false,
                                errorMessage = "Sync warning: $error"
                            )
                            Timber.w(result.exceptionOrNull(), "Data sync had issues")
                        }
                    }
                } catch (e: Exception) {
                    // Catch any exceptions during the sync operation
                    Timber.e(e, "Error during data sync operation")
                    _uiState.value = _uiState.value.copy(
                        isSyncing = false,
                        syncSuccess = false,
                        errorMessage = "Sync operation error: ${e.message ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                // Catch any exceptions in the outer block
                Timber.e(e, "Critical error during data sync")
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    syncSuccess = false,
                    errorMessage = "Critical sync error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }
}
