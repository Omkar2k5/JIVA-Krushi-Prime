package com.example.jiva

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.User
import com.example.jiva.data.repository.AuthRepository
import com.example.jiva.utils.PerformanceMonitor
import com.example.jiva.utils.SecurityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null,
    val attemptCount: Int = 0,
    val isRateLimited: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val rateLimiter = SecurityUtils.RateLimiter(maxAttempts = 5, timeWindowMs = 30000L)

    fun updateUsername(username: String) {
        val sanitizedUsername = SecurityUtils.sanitizeInput(username)
        _uiState.value = _uiState.value.copy(
            username = sanitizedUsername,
            errorMessage = null
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    fun login() {
        val currentState = _uiState.value
        val userIdentifier = currentState.username.lowercase()

        // Check rate limiting
        if (!rateLimiter.isAllowed(userIdentifier)) {
            val remainingTime = rateLimiter.getRemainingTime(userIdentifier)
            _uiState.value = currentState.copy(
                errorMessage = "Too many login attempts. Please wait ${remainingTime / 1000} seconds.",
                isRateLimited = true
            )
            return
        }

        // Validate input format
        if (currentState.username.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Username is required"
            )
            return
        }

        if (!SecurityUtils.isValidUsername(currentState.username)) {
            _uiState.value = currentState.copy(
                errorMessage = "Invalid username format"
            )
            return
        }

        if (currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Password is required"
            )
            return
        }

        if (!SecurityUtils.isValidPassword(currentState.password)) {
            _uiState.value = currentState.copy(
                errorMessage = "Password must be at least 6 characters"
            )
            return
        }

        // Set loading state
        _uiState.value = currentState.copy(
            isLoading = true,
            errorMessage = null,
            isRateLimited = false
        )

        viewModelScope.launch {
            try {
                PerformanceMonitor.logMemoryUsage("Login Start")

                val loginRequest = LoginRequest(
                    username = currentState.username,
                    password = currentState.password
                )

                // Record login attempt for rate limiting
                rateLimiter.recordAttempt(userIdentifier)

                val result = PerformanceMonitor.measureNetworkOperation("User Login") {
                    authRepository.login(loginRequest)
                }

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.user != null) {
                            Timber.d("Login successful for user: ${response.user.username}")
                            _uiState.value = currentState.copy(
                                isLoading = false,
                                isLoginSuccessful = true,
                                errorMessage = null,
                                user = response.user,
                                attemptCount = 0
                            )
                            PerformanceMonitor.logMemoryUsage("Login Success")
                        } else {
                            handleLoginFailure(response.message ?: "Login failed")
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Login error")
                        handleLoginFailure("Network error. Please try again.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected login error")
                handleLoginFailure("An unexpected error occurred")
            }
        }
    }

    private fun handleLoginFailure(message: String) {
        val currentState = _uiState.value
        val newAttemptCount = currentState.attemptCount + 1

        _uiState.value = currentState.copy(
            isLoading = false,
            isLoginSuccessful = false,
            errorMessage = message,
            attemptCount = newAttemptCount,
            isRateLimited = newAttemptCount >= 5
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetLoginSuccess() {
        _uiState.value = _uiState.value.copy(isLoginSuccessful = false)
    }

    fun resetRateLimit() {
        _uiState.value = _uiState.value.copy(
            attemptCount = 0,
            isRateLimited = false,
            errorMessage = null
        )
    }
}
