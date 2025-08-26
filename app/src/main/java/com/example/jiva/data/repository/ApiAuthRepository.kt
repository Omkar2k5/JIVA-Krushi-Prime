package com.example.jiva.data.repository

import com.example.jiva.data.api.JivaApiService
import com.example.jiva.data.api.models.ApiLoginRequest
import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.LoginResponse
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserRole
import com.example.jiva.data.model.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Real AuthRepository implementation that calls the HTTP API.
 */
class ApiAuthRepository(
    private val api: JivaApiService
) : AuthRepository {

    private val _currentSession = MutableStateFlow<UserSession?>(null)

    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            val apiRes = api.login(ApiLoginRequest(mobileNo = request.username, password = request.password))
            if (apiRes.isSuccess && apiRes.message?.equals("success", ignoreCase = true) == true && apiRes.data?.userID != null) {
                val user = User(
                    id = apiRes.data.userID.toLongOrNull() ?: 0L,
                    username = request.username,
                    email = null,
                    firstName = null,
                    lastName = null,
                    role = UserRole.USER
                )
                val session = UserSession(
                    user = user,
                    token = apiRes.data.userID ?: "",
                    expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L,
                )
                _currentSession.value = session
                Result.success(
                    LoginResponse(
                        success = true,
                        user = user,
                        token = session.token,
                        message = apiRes.message,
                        expiresAt = session.expiresAt
                    )
                )
            } else {
                Result.success(
                    LoginResponse(
                        success = false,
                        message = apiRes.message ?: "Login failed"
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> {
        _currentSession.value = null
        return Result.success(Unit)
    }

    override suspend fun getCurrentSession(): UserSession? = _currentSession.value

    override fun observeCurrentSession(): Flow<UserSession?> = _currentSession.asStateFlow()

    override suspend fun isSessionValid(): Boolean {
        val s = _currentSession.value
        return s != null && s.expiresAt > System.currentTimeMillis() && s.isValid
    }

    override suspend fun refreshSession(): Result<UserSession> {
        val s = _currentSession.value ?: return Result.failure(IllegalStateException("No session"))
        val refreshed = s.copy(expiresAt = System.currentTimeMillis() + 24 * 60 * 60 * 1000L)
        _currentSession.value = refreshed
        return Result.success(refreshed)
    }

    override suspend fun getUserByUsername(username: String): User? = _currentSession.value?.user?.takeIf { it.username == username }

    override suspend fun clearSession() { _currentSession.value = null }
}