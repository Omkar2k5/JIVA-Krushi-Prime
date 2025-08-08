package com.example.jiva.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Secure credential manager for storing and retrieving login credentials
 * Uses Android's EncryptedSharedPreferences for secure storage
 */
class CredentialManager private constructor(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "jiva_secure_credentials"
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_REMEMBER_ME = "remember_me"
        private const val KEY_AUTO_LOGIN = "auto_login"
        private const val KEY_LAST_LOGIN_TIME = "last_login_time"
        
        @Volatile
        private var INSTANCE: CredentialManager? = null
        
        fun getInstance(context: Context): CredentialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CredentialManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    /**
     * Data class to hold login credentials
     */
    data class LoginCredentials(
        val username: String,
        val password: String,
        val rememberMe: Boolean = false,
        val autoLogin: Boolean = false
    )
    
    /**
     * Save login credentials securely
     */
    suspend fun saveCredentials(
        username: String,
        password: String,
        rememberMe: Boolean = false,
        autoLogin: Boolean = false
    ) = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit().apply {
                putString(KEY_USERNAME, username)
                putString(KEY_PASSWORD, password)
                putBoolean(KEY_REMEMBER_ME, rememberMe)
                putBoolean(KEY_AUTO_LOGIN, autoLogin)
                putLong(KEY_LAST_LOGIN_TIME, System.currentTimeMillis())
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Retrieve saved credentials
     */
    suspend fun getCredentials(): LoginCredentials? = withContext(Dispatchers.IO) {
        try {
            val username = encryptedPrefs.getString(KEY_USERNAME, null)
            val password = encryptedPrefs.getString(KEY_PASSWORD, null)
            val rememberMe = encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
            val autoLogin = encryptedPrefs.getBoolean(KEY_AUTO_LOGIN, false)
            
            if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                LoginCredentials(username, password, rememberMe, autoLogin)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Check if auto-login is enabled and credentials exist
     */
    suspend fun shouldAutoLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            val autoLogin = encryptedPrefs.getBoolean(KEY_AUTO_LOGIN, false)
            val hasCredentials = hasValidCredentials()
            autoLogin && hasCredentials
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Check if valid credentials exist
     */
    suspend fun hasValidCredentials(): Boolean = withContext(Dispatchers.IO) {
        try {
            val username = encryptedPrefs.getString(KEY_USERNAME, null)
            val password = encryptedPrefs.getString(KEY_PASSWORD, null)
            !username.isNullOrEmpty() && !password.isNullOrEmpty()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get saved username only (for pre-filling login form)
     */
    suspend fun getSavedUsername(): String? = withContext(Dispatchers.IO) {
        try {
            val rememberMe = encryptedPrefs.getBoolean(KEY_REMEMBER_ME, false)
            if (rememberMe) {
                encryptedPrefs.getString(KEY_USERNAME, null)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Clear all saved credentials (logout)
     */
    suspend fun clearCredentials() = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit().apply {
                remove(KEY_USERNAME)
                remove(KEY_PASSWORD)
                remove(KEY_REMEMBER_ME)
                remove(KEY_AUTO_LOGIN)
                remove(KEY_LAST_LOGIN_TIME)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Update auto-login preference
     */
    suspend fun setAutoLogin(enabled: Boolean) = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.edit().apply {
                putBoolean(KEY_AUTO_LOGIN, enabled)
                apply()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Get last login time
     */
    suspend fun getLastLoginTime(): Long = withContext(Dispatchers.IO) {
        try {
            encryptedPrefs.getLong(KEY_LAST_LOGIN_TIME, 0L)
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
    
    /**
     * Check if credentials are expired (optional security feature)
     */
    suspend fun areCredentialsExpired(expiryDays: Int = 30): Boolean = withContext(Dispatchers.IO) {
        try {
            val lastLoginTime = getLastLoginTime()
            if (lastLoginTime == 0L) return@withContext true
            
            val currentTime = System.currentTimeMillis()
            val expiryTime = lastLoginTime + (expiryDays * 24 * 60 * 60 * 1000L)
            
            currentTime > expiryTime
        } catch (e: Exception) {
            e.printStackTrace()
            true
        }
    }
}
