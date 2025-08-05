package com.example.jiva.data.repository

import com.example.jiva.data.model.LoginRequest
import com.example.jiva.data.model.LoginResponse
import com.example.jiva.data.model.User
import com.example.jiva.data.model.UserSession
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for authentication operations
 * This interface will be implemented by both dummy data source and real SQL database
 */
interface AuthRepository {
    
    /**
     * Authenticate user with username and password
     */
    suspend fun login(request: LoginRequest): Result<LoginResponse>
    
    /**
     * Logout current user
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Get current user session
     */
    suspend fun getCurrentSession(): UserSession?
    
    /**
     * Observe current user session changes
     */
    fun observeCurrentSession(): Flow<UserSession?>
    
    /**
     * Validate if current session is still valid
     */
    suspend fun isSessionValid(): Boolean
    
    /**
     * Refresh user session/token
     */
    suspend fun refreshSession(): Result<UserSession>
    
    /**
     * Get user by username (for validation)
     */
    suspend fun getUserByUsername(username: String): User?
    
    /**
     * Clear all stored session data
     */
    suspend fun clearSession()
}
