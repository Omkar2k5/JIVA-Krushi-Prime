package com.example.jiva.ui.theme

import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
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
     * Ensure minimum touch target size for interactive elements
     */
    fun Modifier.minTouchTargetSize(): Modifier {
        return this.size(MinTouchTargetSize)
    }
    
    /**
     * Add semantic content description for screen readers
     */
    fun Modifier.contentDescription(description: String): Modifier {
        return this.semantics {
            contentDescription = description
        }
    }
    
    /**
     * Get high contrast color for better visibility
     */
    @Composable
    fun getHighContrastColor(
        defaultColor: Color,
        isHighContrast: Boolean = false
    ): Color {
        return if (isHighContrast) {
            if (defaultColor.luminance() > 0.5f) {
                Color.Black
            } else {
                Color.White
            }
        } else {
            defaultColor
        }
    }
    
    /**
     * Check if color combination has sufficient contrast ratio
     */
    fun hasGoodContrast(
        foreground: Color,
        background: Color,
        minRatio: Float = 4.5f
    ): Boolean {
        val contrastRatio = calculateContrastRatio(foreground, background)
        return contrastRatio >= minRatio
    }
    
    /**
     * Calculate contrast ratio between two colors
     */
    private fun calculateContrastRatio(color1: Color, color2: Color): Float {
        val luminance1 = color1.luminance()
        val luminance2 = color2.luminance()
        
        val lighter = maxOf(luminance1, luminance2)
        val darker = minOf(luminance1, luminance2)
        
        return (lighter + 0.05f) / (darker + 0.05f)
    }
    
    /**
     * Get relative luminance of a color
     */
    private fun Color.luminance(): Float {
        fun componentLuminance(component: Float): Float {
            return if (component <= 0.03928f) {
                component / 12.92f
            } else {
                kotlin.math.pow((component + 0.055f) / 1.055f, 2.4f).toFloat()
            }
        }
        
        val r = componentLuminance(red)
        val g = componentLuminance(green)
        val b = componentLuminance(blue)
        
        return 0.2126f * r + 0.7152f * g + 0.0722f * b
    }
    
    /**
     * Accessibility announcements for screen readers
     */
    object Announcements {
        const val LOGIN_SUCCESS = "Login successful. Navigating to home screen."
        const val LOGIN_FAILED = "Login failed. Please check your credentials and try again."
        const val LOADING = "Loading. Please wait."
        const val FORM_ERROR = "Form contains errors. Please review and correct them."
        const val RATE_LIMITED = "Too many login attempts. Please wait before trying again."
        const val LOGOUT_SUCCESS = "Logged out successfully."
        const val SESSION_EXPIRED = "Your session has expired. Please log in again."
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
        const val USER_AVATAR = "User profile picture"
        const val NAVIGATION_MENU = "Navigation menu"
        const val CLOSE_DIALOG = "Close dialog"
        const val DISMISS_SNACKBAR = "Dismiss notification"
    }
    
    /**
     * Semantic roles for better screen reader navigation
     */
    object Roles {
        const val BUTTON = "button"
        const val TEXT_FIELD = "text field"
        const val HEADING = "heading"
        const val LINK = "link"
        const val IMAGE = "image"
        const val LIST = "list"
        const val LIST_ITEM = "list item"
        const val DIALOG = "dialog"
        const val ALERT = "alert"
        const val STATUS = "status"
    }
}
