package com.example.jiva

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.User
import com.example.jiva.data.repository.AuthRepository
import com.example.jiva.utils.FileCredentialManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isLoginSuccessful: Boolean = false,
    val errorMessage: String? = null,
    val user: User? = null,
    val attemptCount: Int = 0,
    val isRateLimited: Boolean = false,
    val rememberMe: Boolean = false,
    val autoLogin: Boolean = false,
    val isAutoLoggingIn: Boolean = false
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val context: Context? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val maxLoginAttempts = 5
    private var lastAttemptTime = 0L
    private val rateLimitDuration = 30000L // 30 seconds

    private val fileCredentialManager = context?.let { FileCredentialManager.getInstance(it) }

    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(
            username = username.trim(),
            errorMessage = null
        )
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(
            password = password,
            errorMessage = null
        )
    }

    fun updateRememberMe(rememberMe: Boolean) {
        _uiState.value = _uiState.value.copy(
            rememberMe = rememberMe,
            errorMessage = null
        )
    }

    fun updateAutoLogin(autoLogin: Boolean) {
        _uiState.value = _uiState.value.copy(
            autoLogin = autoLogin,
            errorMessage = null
        )
    }

    fun login() {
        val currentState = _uiState.value

        // Check rate limiting
        if (isRateLimited()) {
            _uiState.value = currentState.copy(
                errorMessage = "Too many login attempts. Please wait 30 seconds.",
                isRateLimited = true
            )
            return
        }

        // Validate input
        if (currentState.username.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Username is required"
            )
            return
        }

        if (currentState.password.isBlank()) {
            _uiState.value = currentState.copy(
                errorMessage = "Password is required"
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
                val loginRequest = LoginRequest(
                    username = currentState.username,
                    password = currentState.password
                )

                val result = authRepository.login(loginRequest)

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

                            // Store userID globally for later API calls
                            response.user?.let { u ->
                                context?.let { ctx ->
                                    com.example.jiva.utils.UserEnv.setUserId(ctx, u.id.toString())
                                }
                            }

                            // Save credentials if remember me or auto-login is enabled
                            viewModelScope.launch {
                                saveCredentialsIfNeeded()
                            }

                            // Fetch company info and store companyName
                            viewModelScope.launch {
                                try {
                                    val uidStr = response.user?.id?.toString()
                                    val uid = uidStr?.toIntOrNull()
                                    if (uid != null) {
                                        val api = com.example.jiva.data.network.RetrofitClient.jivaApiService
                                        val companyRes = api.getCompanyInfo(
                                            com.example.jiva.data.api.models.CompanyInfoRequest(userId = uid)
                                        )
                                        if (companyRes.isSuccess) {
                                            val name = companyRes.data?.companyName?.takeIf { !it.isNullOrBlank() }
                                            if (name != null) {
                                                context?.let { ctx ->
                                                    com.example.jiva.utils.UserEnv.setCompanyName(ctx, name)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "CompanyInfo fetch failed")
                                }
                            }
                        } else {
                            handleLoginFailure(response.message ?: "Login failed")
                        }
                    },
                    onFailure = { exception ->
                        Timber.e(exception, "Login error")
                        val msg = exception.message?.takeIf { it.isNotBlank() } ?: "Network error. Please try again."
                        handleLoginFailure(msg)
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
        lastAttemptTime = System.currentTimeMillis()

        _uiState.value = currentState.copy(
            isLoading = false,
            isLoginSuccessful = false,
            errorMessage = message,
            attemptCount = newAttemptCount,
            isRateLimited = newAttemptCount >= maxLoginAttempts
        )
    }

    private fun isRateLimited(): Boolean {
        val currentState = _uiState.value
        return currentState.attemptCount >= maxLoginAttempts &&
               (System.currentTimeMillis() - lastAttemptTime) < rateLimitDuration
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

    /**
     * Initialize the ViewModel and check for saved credentials
     */
    init {
        loadSavedCredentials()
    }

    /**
     * Load saved credentials and check for auto-login
     */
    private fun loadSavedCredentials() {
        fileCredentialManager?.let { manager ->
            viewModelScope.launch {
                try {
                    val credentials = manager.loadCredentials()
                    if (credentials != null) {
                        _uiState.value = _uiState.value.copy(
                            username = if (credentials.rememberMe) credentials.username else "",
                            rememberMe = credentials.rememberMe,
                            autoLogin = credentials.autoLogin
                        )

                        // Check if auto-login should be performed
                        if (credentials.autoLogin && manager.shouldAutoLogin()) {
                            performAutoLogin(credentials)
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error loading saved credentials")
                }
            }
        }
    }

    /**
     * Perform automatic login with saved credentials
     */
    private fun performAutoLogin(credentials: FileCredentialManager.UserCredentials) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isAutoLoggingIn = true,
                    isLoading = true,
                    username = credentials.username,
                    password = credentials.password
                )

                val loginRequest = LoginRequest(
                    username = credentials.username,
                    password = credentials.password
                )

                val result = authRepository.login(loginRequest)

                result.fold(
                    onSuccess = { response ->
                        if (response.success && response.user != null) {
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAutoLoggingIn = false,
                                isLoginSuccessful = true,
                                user = response.user,
                                errorMessage = null
                            )

                            // Store userID globally for later API calls
                            response.user?.let { u ->
                                context?.let { ctx ->
                                    com.example.jiva.utils.UserEnv.setUserId(ctx, u.id.toString())
                                }
                            }

                            // Fetch company info and store companyName
                            viewModelScope.launch {
                                try {
                                    val uidStr = response.user?.id?.toString()
                                    val uid = uidStr?.toIntOrNull()
                                    if (uid != null) {
                                        val api = com.example.jiva.data.network.RetrofitClient.jivaApiService
                                        val companyRes = api.getCompanyInfo(
                                            com.example.jiva.data.api.models.CompanyInfoRequest(userId = uid)
                                        )
                                        if (companyRes.isSuccess) {
                                            val name = companyRes.data?.companyName?.takeIf { !it.isNullOrBlank() }
                                            if (name != null) {
                                                context?.let { ctx ->
                                                    com.example.jiva.utils.UserEnv.setCompanyName(ctx, name)
                                                }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    Timber.w(e, "CompanyInfo fetch failed")
                                }
                            }

                            Timber.d("Auto-login successful for user: ${response.user.username}")
                        } else {
                            // Auto-login failed, clear credentials and show login form
                            fileCredentialManager?.clearCredentials()
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                isAutoLoggingIn = false,
                                password = "", // Clear password for security
                                autoLogin = false,
                                errorMessage = "Auto-login failed. Please login again."
                            )
                            Timber.w("Auto-login failed, credentials cleared")
                        }
                    },
                    onFailure = { exception ->
                        // Auto-login failed, clear credentials and show login form
                        fileCredentialManager?.clearCredentials()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isAutoLoggingIn = false,
                            password = "", // Clear password for security
                            autoLogin = false,
                            errorMessage = "Auto-login failed. Please login again."
                        )
                        Timber.w("Auto-login failed with exception: ${exception.message}")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Auto-login error")
                fileCredentialManager?.clearCredentials()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isAutoLoggingIn = false,
                    password = "",
                    errorMessage = "Auto-login failed. Please login again."
                )
            }
        }
    }

    /**
     * Save credentials after successful login
     */
    private suspend fun saveCredentialsIfNeeded() {
        val currentState = _uiState.value
        if (currentState.rememberMe || currentState.autoLogin) {
            fileCredentialManager?.let { manager ->
                val success = manager.saveCredentials(
                    username = currentState.username,
                    password = currentState.password,
                    rememberMe = currentState.rememberMe,
                    autoLogin = currentState.autoLogin
                )

                if (success) {
                    Timber.d("Credentials saved successfully to file")
                } else {
                    Timber.w("Failed to save credentials to file")
                }
            }
        }
    }

    /**
     * Clear saved credentials (logout)
     */
    fun logout() {
        viewModelScope.launch {
            try {
                fileCredentialManager?.clearCredentials()
                _uiState.value = LoginUiState() // Reset to initial state
                Timber.d("Logout successful, credentials file cleared")
            } catch (e: Exception) {
                Timber.e(e, "Error during logout")
            }
        }
    }
}
