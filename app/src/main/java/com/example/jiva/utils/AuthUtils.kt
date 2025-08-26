package com.example.jiva.utils

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Utility functions for authentication operations
 */
object AuthUtils {

    /**
     * Perform logout by clearing saved credentials from file
     */
    fun logout(context: Context, onComplete: () -> Unit = {}) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileCredentialManager = FileCredentialManager.getInstance(context)
                val success = fileCredentialManager.clearCredentials()
                // Also clear environment user id
                UserEnv.clearUserId(context)
                if (success) {
                    Timber.d("Logout: Credentials file cleared successfully")
                } else {
                    Timber.w("Logout: Failed to clear credentials file")
                }
            } catch (e: Exception) {
                Timber.e(e, "Logout: Error clearing credentials file")
            } finally {
                // Switch back to main thread for UI operations
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete()
                }
            }
        }
    }
}
