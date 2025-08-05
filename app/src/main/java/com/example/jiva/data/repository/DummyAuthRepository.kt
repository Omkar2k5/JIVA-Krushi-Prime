package com.example.jiva.data.repository

import com.example.jiva.data.datastore.UserPreferencesDataStore
import com.example.jiva.data.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dummy implementation of AuthRepository for development
 * This will be replaced with your SQL database implementation later
 */
@Singleton
class DummyAuthRepository @Inject constructor(
    private val userPreferencesDataStore: UserPreferencesDataStore
) : AuthRepository {
    
    // Dummy users database - replace with SQL database later
    private val dummyUsers = listOf(
        User(
            id = 1,
            username = "demo",
            email = "demo@jiva.com",
            firstName = "Demo",
            lastName = "User",
            role = UserRole.USER
        ),
        User(
            id = 2,
            username = "admin",
            email = "admin@jiva.com",
            firstName = "Admin",
            lastName = "User",
            role = UserRole.ADMIN
        ),
        User(
            id = 3,
            username = "test",
            email = "test@jiva.com",
            firstName = "Test",
            lastName = "User",
            role = UserRole.USER
        )
    )
    
    // Dummy password storage - in real implementation, use hashed passwords
    private val userPasswords = mapOf(
        "demo" to "testing",
        "admin" to "admin123",
        "test" to "test123"
    )
    
    private val _currentSession = MutableStateFlow<UserSession?>(null)
    
    override suspend fun login(request: LoginRequest): Result<LoginResponse> {
        return try {
            Timber.d("Attempting login for user: ${request.username}")
            
            // Simulate network delay
            delay(1000)
            
            // Rate limiting simulation - in production, implement proper rate limiting
            if (request.username.isBlank() || request.password.isBlank()) {
                return Result.success(
                    LoginResponse(
                        success = false,
                        message = "Username and password are required"
                    )
                )
            }
            
            // Find user
            val user = dummyUsers.find { it.username == request.username }
            val expectedPassword = userPasswords[request.username]
            
            if (user != null && expectedPassword == request.password) {
                // Generate session token
                val token = UUID.randomUUID().toString()
                val expiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000) // 24 hours
                
                val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
                val session = UserSession(
                    user = updatedUser,
                    token = token,
                    expiresAt = expiresAt
                )

                _currentSession.value = session

                // Persist session to DataStore for app restart survival
                userPreferencesDataStore.saveUserSession(session)
                
                Timber.d("Login successful for user: ${request.username}")
                Result.success(
                    LoginResponse(
                        success = true,
                        user = updatedUser,
                        token = token,
                        message = "Login successful",
                        expiresAt = expiresAt
                    )
                )
            } else {
                Timber.w("Login failed for user: ${request.username}")
                Result.success(
                    LoginResponse(
                        success = false,
                        message = "Invalid username or password"
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Login error for user: ${request.username}")
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> {
        return try {
            _currentSession.value = null
            Timber.d("User logged out successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Logout error")
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentSession(): UserSession? {
        return _currentSession.value?.takeIf { isSessionValid(it) }
    }
    
    override fun observeCurrentSession(): Flow<UserSession?> {
        return _currentSession.asStateFlow()
    }
    
    override suspend fun isSessionValid(): Boolean {
        val session = _currentSession.value
        return session != null && isSessionValid(session)
    }
    
    private fun isSessionValid(session: UserSession): Boolean {
        return session.isValid && session.expiresAt > System.currentTimeMillis()
    }
    
    override suspend fun refreshSession(): Result<UserSession> {
        return try {
            val currentSession = _currentSession.value
            if (currentSession != null && isSessionValid(currentSession)) {
                val newExpiresAt = System.currentTimeMillis() + (24 * 60 * 60 * 1000)
                val refreshedSession = currentSession.copy(expiresAt = newExpiresAt)
                _currentSession.value = refreshedSession
                Result.success(refreshedSession)
            } else {
                Result.failure(Exception("No valid session to refresh"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Session refresh error")
            Result.failure(e)
        }
    }
    
    override suspend fun getUserByUsername(username: String): User? {
        return dummyUsers.find { it.username == username }
    }
    
    override suspend fun clearSession() {
        _currentSession.value = null
    }
}
