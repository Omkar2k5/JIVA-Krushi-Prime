package com.example.jiva.ui.theme

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Accessibility utilities for better app usability
 * Essential for production apps with diverse user base
 */
object Accessibility {
    
    // Minimum touch target size as per Material Design guidelines
    val MinTouchTargetSize = 48.dp
    
    // Recommended spacing for better accessibility
    val AccessibleSpacing = 16.dp

    /**
     * Add semantic content description for screen readers
     */
    fun Modifier.contentDescription(description: String): Modifier {
        return this.semantics {
            contentDescription = description
        }
    }
    
    /**
     * Content descriptions for UI elements
     */
    object ContentDescriptions {
        const val USERNAME_FIELD = "Username input field"
        const val PASSWORD_FIELD = "Password input field"
        const val LOGIN_BUTTON = "Login button"
        const val LOGOUT_BUTTON = "Logout button"
        const val SHOW_PASSWORD = "Show password"
        const val HIDE_PASSWORD = "Hide password"
        const val LOADING_INDICATOR = "Loading in progress"
        const val ERROR_MESSAGE = "Error message"
        const val SUCCESS_MESSAGE = "Success message"
    }
}
