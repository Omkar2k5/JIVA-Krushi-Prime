package com.example.jiva.utils

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import timber.log.Timber
import java.security.MessageDigest
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Security utilities for production-ready authentication and data protection
 * Essential for handling sensitive user data with 100+ concurrent users
 */
object SecurityUtils {
    
    private const val ENCRYPTED_PREFS_NAME = "jiva_secure_prefs"
    private const val AES_TRANSFORMATION = "AES/CBC/PKCS7Padding"
    
    /**
     * Input validation patterns
     */
    private val USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9._-]{3,20}$")
    private val EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PASSWORD_PATTERN = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[a-zA-Z\\d@\$!%*?&]{8,}$")
    
    /**
     * Validate username format
     */
    fun isValidUsername(username: String): Boolean {
        return username.isNotBlank() && USERNAME_PATTERN.matcher(username).matches()
    }
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && EMAIL_PATTERN.matcher(email).matches()
    }
    
    /**
     * Validate password strength
     * For production: at least 8 characters, 1 uppercase, 1 lowercase, 1 digit
     */
    fun isValidPassword(password: String): Boolean {
        return password.isNotBlank() && password.length >= 6 // Relaxed for demo
    }
    
    /**
     * Validate password strength (strict for production)
     */
    fun isStrongPassword(password: String): Boolean {
        return password.isNotBlank() && PASSWORD_PATTERN.matcher(password).matches()
    }
    
    /**
     * Sanitize input to prevent injection attacks
     */
    fun sanitizeInput(input: String): String {
        return input.trim()
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")
            .replace("/", "&#x2F;")
    }
    
    /**
     * Hash password using SHA-256 (for demo purposes)
     * In production, use bcrypt, scrypt, or Argon2
     */
    fun hashPassword(password: String, salt: String = "jiva_salt"): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val saltedPassword = password + salt
            val hashBytes = digest.digest(saltedPassword.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error hashing password")
            ""
        }
    }
    
    /**
     * Generate secure random token
     */
    fun generateSecureToken(): String {
        return try {
            val random = java.security.SecureRandom()
            val bytes = ByteArray(32)
            random.nextBytes(bytes)
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Timber.e(e, "Error generating secure token")
            java.util.UUID.randomUUID().toString()
        }
    }
    
    /**
     * Create encrypted shared preferences for sensitive data
     */
    fun createEncryptedPreferences(context: Context): android.content.SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                ENCRYPTED_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Error creating encrypted preferences")
            null
        }
    }
    
    /**
     * Check if device is rooted (basic check)
     */
    fun isDeviceRooted(): Boolean {
        return try {
            val buildTags = Build.TAGS
            buildTags != null && buildTags.contains("test-keys")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if app is running in debug mode
     */
    fun isDebuggable(context: Context): Boolean {
        return try {
            val appInfo = context.applicationInfo
            (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Rate limiting for login attempts
     */
    class RateLimiter(
        private val maxAttempts: Int = 5,
        private val timeWindowMs: Long = 300000L // 5 minutes
    ) {
        private val attempts = mutableMapOf<String, MutableList<Long>>()
        
        fun isAllowed(identifier: String): Boolean {
            val now = System.currentTimeMillis()
            val userAttempts = attempts.getOrPut(identifier) { mutableListOf() }
            
            // Remove old attempts outside the time window
            userAttempts.removeAll { it < now - timeWindowMs }
            
            return userAttempts.size < maxAttempts
        }
        
        fun recordAttempt(identifier: String) {
            val now = System.currentTimeMillis()
            val userAttempts = attempts.getOrPut(identifier) { mutableListOf() }
            userAttempts.add(now)
        }
        
        fun getRemainingTime(identifier: String): Long {
            val userAttempts = attempts[identifier] ?: return 0L
            if (userAttempts.size < maxAttempts) return 0L
            
            val oldestAttempt = userAttempts.minOrNull() ?: return 0L
            val now = System.currentTimeMillis()
            return maxOf(0L, timeWindowMs - (now - oldestAttempt))
        }
    }
    
    /**
     * Session token validation
     */
    fun isValidSessionToken(token: String): Boolean {
        return token.isNotBlank() && token.length >= 32
    }
    
    /**
     * Check if session is expired
     */
    fun isSessionExpired(expiresAt: Long): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Security headers for network requests
     */
    object NetworkSecurity {
        const val USER_AGENT = "JIVA-Android/1.0"
        const val CONTENT_TYPE = "application/json"
        const val ACCEPT = "application/json"
        
        fun getSecurityHeaders(): Map<String, String> {
            return mapOf(
                "User-Agent" to USER_AGENT,
                "Content-Type" to CONTENT_TYPE,
                "Accept" to ACCEPT,
                "X-Requested-With" to "XMLHttpRequest",
                "Cache-Control" to "no-cache, no-store, must-revalidate",
                "Pragma" to "no-cache"
            )
        }
    }
}
