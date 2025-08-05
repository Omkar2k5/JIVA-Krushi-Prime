package com.example.jiva

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.model.User
import com.example.jiva.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class HomeUiState(
    val currentUser: User? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {
    
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
}
