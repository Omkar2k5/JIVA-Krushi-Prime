package com.example.jiva.utils

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Properties

/**
 * Simple file-based credential manager for auto-login functionality
 * Stores credentials in internal app storage for security
 */
class FileCredentialManager private constructor(private val context: Context) {
    
    companion object {
        private const val CREDENTIALS_FILE_NAME = "jiva_credentials.properties"
    private const val KEY_USERNAME = "username"
    private const val KEY_PASSWORD = "password"
    private const val KEY_AUTO_LOGIN = "auto_login"
    private const val KEY_REMEMBER_ME = "remember_me"
    private const val KEY_LAST_LOGIN_TIME = "last_login_time"
    private const val KEY_AUTO_LOGIN_ONLY = "auto_login_only"
        
        @Volatile
        private var INSTANCE: FileCredentialManager? = null
        
        fun getInstance(context: Context): FileCredentialManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FileCredentialManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val credentialsFile = File(context.filesDir, CREDENTIALS_FILE_NAME)
    
    /**
     * Data class to hold login credentials
     */
    data class UserCredentials(
        val username: String,
        val password: String,
        val autoLogin: Boolean = false,
        val rememberMe: Boolean = false,
        val lastLoginTime: Long = 0L
    )
    
    /**
     * Save user credentials to file
     */
    suspend fun saveCredentials(
        username: String,
        password: String,
        autoLogin: Boolean = false,
        rememberMe: Boolean = false
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val properties = Properties()
            properties.setProperty(KEY_USERNAME, username)
            properties.setProperty(KEY_PASSWORD, password)
            properties.setProperty(KEY_AUTO_LOGIN, autoLogin.toString())
            properties.setProperty(KEY_REMEMBER_ME, rememberMe.toString())
            properties.setProperty(KEY_LAST_LOGIN_TIME, System.currentTimeMillis().toString())
            
            FileOutputStream(credentialsFile).use { output ->
                properties.store(output, "JIVA User Credentials")
            }
            
            Timber.d("Credentials saved successfully to file")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving credentials to file")
            false
        }
    }
    
    /**
     * Load user credentials from file
     */
    suspend fun loadCredentials(): UserCredentials? = withContext(Dispatchers.IO) {
        try {
            if (!credentialsFile.exists()) {
                Timber.d("Credentials file does not exist")
                return@withContext null
            }
            
            val properties = Properties()
            FileInputStream(credentialsFile).use { input ->
                properties.load(input)
            }
            
            val username = properties.getProperty(KEY_USERNAME)
            val password = properties.getProperty(KEY_PASSWORD)
            val autoLogin = properties.getProperty(KEY_AUTO_LOGIN, "false").toBoolean()
            val rememberMe = properties.getProperty(KEY_REMEMBER_ME, "false").toBoolean()
            val lastLoginTime = properties.getProperty(KEY_LAST_LOGIN_TIME, "0").toLong()
            
            if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                Timber.d("Credentials loaded successfully from file")
                UserCredentials(
                    username = username,
                    password = password,
                    autoLogin = autoLogin,
                    rememberMe = rememberMe,
                    lastLoginTime = lastLoginTime
                )
            } else {
                Timber.w("Invalid credentials found in file")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error loading credentials from file")
            null
        }
    }
    
    /**
     * Check if auto-login should be performed (new approach - only check preference)
     */
    suspend fun shouldAutoLogin(): Boolean = withContext(Dispatchers.IO) {
        try {
            // First check if auto-login preference is enabled
            val autoLoginEnabled = getAutoLoginPreference()
            if (!autoLoginEnabled) {
                Timber.d("Auto-login disabled in preferences")
                return@withContext false
            }
            
            // If auto-login is enabled, we should attempt it with API call
            val credentials = loadCredentials()
            val result = credentials != null && 
                        !credentials.username.isNullOrEmpty() && 
                        !credentials.password.isNullOrEmpty()
            
            Timber.d("Should auto-login: $result (preference enabled: $autoLoginEnabled)")
            result
        } catch (e: Exception) {
            Timber.e(e, "Error checking auto-login status")
            false
        }
    }
    
    /**
     * Get saved username for pre-filling login form
     */
    suspend fun getSavedUsername(): String? = withContext(Dispatchers.IO) {
        try {
            val credentials = loadCredentials()
            if (credentials?.rememberMe == true) {
                credentials.username
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error getting saved username")
            null
        }
    }
    
    /**
     * Clear all saved credentials (logout)
     */
    suspend fun clearCredentials(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (credentialsFile.exists()) {
                val deleted = credentialsFile.delete()
                if (deleted) {
                    Timber.d("Credentials file deleted successfully")
                } else {
                    Timber.w("Failed to delete credentials file")
                }
                deleted
            } else {
                Timber.d("Credentials file does not exist, nothing to clear")
                true
            }
        } catch (e: Exception) {
            Timber.e(e, "Error clearing credentials file")
            false
        }
    }
    
    /**
     * Check if credentials file exists
     */
    suspend fun hasCredentials(): Boolean = withContext(Dispatchers.IO) {
        try {
            credentialsFile.exists() && loadCredentials() != null
        } catch (e: Exception) {
            Timber.e(e, "Error checking if credentials exist")
            false
        }
    }
    
    /**
     * Update auto-login preference only
     */
    suspend fun updateAutoLogin(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val credentials = loadCredentials()
            if (credentials != null) {
                saveCredentials(
                    username = credentials.username,
                    password = credentials.password,
                    autoLogin = enabled,
                    rememberMe = credentials.rememberMe
                )
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating auto-login preference")
            false
        }
    }
    
    /**
     * Update remember me preference only
     */
    suspend fun updateRememberMe(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val credentials = loadCredentials()
            if (credentials != null) {
                saveCredentials(
                    username = credentials.username,
                    password = credentials.password,
                    autoLogin = credentials.autoLogin,
                    rememberMe = enabled
                )
            } else {
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "Error updating remember me preference")
            false
        }
    }
    
    /**
     * Save only auto-login preference to permanent storage (new approach)
     */
    suspend fun saveAutoLoginPreference(enabled: Boolean): Boolean = withContext(Dispatchers.IO) {
        try {
            val properties = Properties()
            
            // Load existing properties if file exists
            if (credentialsFile.exists()) {
                FileInputStream(credentialsFile).use { input ->
                    properties.load(input)
                }
            }
            
            // Update only the auto-login preference
            properties.setProperty(KEY_AUTO_LOGIN_ONLY, enabled.toString())
            
            FileOutputStream(credentialsFile).use { output ->
                properties.store(output, "JIVA Auto-Login Preference")
            }
            
            Timber.d("Auto-login preference saved: $enabled")
            true
        } catch (e: Exception) {
            Timber.e(e, "Error saving auto-login preference")
            false
        }
    }
    
    /**
     * Get auto-login preference from permanent storage
     */
    suspend fun getAutoLoginPreference(): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!credentialsFile.exists()) {
                return@withContext false
            }
            
            val properties = Properties()
            FileInputStream(credentialsFile).use { input ->
                properties.load(input)
            }
            
            val autoLoginOnly = properties.getProperty(KEY_AUTO_LOGIN_ONLY, "false").toBoolean()
            Timber.d("Auto-login preference loaded: $autoLoginOnly")
            autoLoginOnly
        } catch (e: Exception) {
            Timber.e(e, "Error loading auto-login preference")
            false
        }
    }
}
