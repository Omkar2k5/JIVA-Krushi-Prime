package com.example.jiva.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Utility functions for authentication operations
 */
object AuthUtils {
    
    /**
     * Perform logout by clearing saved credentials
     */
    fun logout(context: Context, onComplete: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val credentialManager = CredentialManager.getInstance(context)
                credentialManager.clearCredentials()
                println("Logout: Credentials cleared successfully")
            } catch (e: Exception) {
                println("Logout: Error clearing credentials - ${e.message}")
            } finally {
                // Switch back to main thread for UI operations
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete()
                }
            }
        }
    }
}
