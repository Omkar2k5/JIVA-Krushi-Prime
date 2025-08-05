package com.example.jiva.data.model

/**
 * User data model that represents a user in the system
 * This will map to your SQL database table structure later
 */
data class User(
    val id: Long = 0,
    val username: String,
    val email: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val role: UserRole = UserRole.USER
)

enum class UserRole {
    USER,
    ADMIN,
    MODERATOR
}

/**
 * Authentication request model
 */
data class LoginRequest(
    val username: String,
    val password: String
)

/**
 * Authentication response model
 */
data class LoginResponse(
    val success: Boolean,
    val user: User? = null,
    val token: String? = null,
    val message: String? = null,
    val expiresAt: Long? = null
)

/**
 * Session data model for storing user session information
 */
data class UserSession(
    val user: User,
    val token: String,
    val expiresAt: Long,
    val isValid: Boolean = true
)
