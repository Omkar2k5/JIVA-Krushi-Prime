package com.example.jiva.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility class for handling different screen sizes and orientations
 * Optimized for Android 7-15 compatibility
 */
object ScreenUtils {
    
    /**
     * Screen size categories based on Android design guidelines
     */
    enum class ScreenSize {
        SMALL,      // < 600dp width
        MEDIUM,     // 600dp - 840dp width  
        LARGE,      // > 840dp width
        EXTRA_LARGE // > 1200dp width
    }
    
    /**
     * Device orientation
     */
    enum class Orientation {
        PORTRAIT,
        LANDSCAPE
    }
    
    /**
     * Get current screen size category
     */
    @Composable
    fun getScreenSize(): ScreenSize {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp
        
        return when {
            screenWidthDp < 600 -> ScreenSize.SMALL
            screenWidthDp < 840 -> ScreenSize.MEDIUM
            screenWidthDp < 1200 -> ScreenSize.LARGE
            else -> ScreenSize.EXTRA_LARGE
        }
    }
    
    /**
     * Get current orientation
     */
    @Composable
    fun getOrientation(): Orientation {
        val configuration = LocalConfiguration.current
        return if (configuration.screenWidthDp > configuration.screenHeightDp) {
            Orientation.LANDSCAPE
        } else {
            Orientation.PORTRAIT
        }
    }
    
    /**
     * Get responsive padding based on screen size
     */
    @Composable
    fun getResponsivePadding(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 12.dp
            ScreenSize.MEDIUM -> 16.dp
            ScreenSize.LARGE -> 20.dp
            ScreenSize.EXTRA_LARGE -> 24.dp
        }
    }
    
    /**
     * Get responsive card padding based on screen size
     */
    @Composable
    fun getCardPadding(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 16.dp
            ScreenSize.MEDIUM -> 20.dp
            ScreenSize.LARGE -> 24.dp
            ScreenSize.EXTRA_LARGE -> 28.dp
        }
    }
    
    /**
     * Get responsive font size multiplier
     */
    @Composable
    fun getFontSizeMultiplier(): Float {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 0.9f
            ScreenSize.MEDIUM -> 1.0f
            ScreenSize.LARGE -> 1.1f
            ScreenSize.EXTRA_LARGE -> 1.2f
        }
    }
    
    /**
     * Get number of grid columns based on screen size
     */
    @Composable
    fun getGridColumns(): Int {
        val screenSize = getScreenSize()
        val orientation = getOrientation()
        
        return when (screenSize) {
            ScreenSize.SMALL -> if (orientation == Orientation.PORTRAIT) 2 else 3
            ScreenSize.MEDIUM -> if (orientation == Orientation.PORTRAIT) 3 else 4
            ScreenSize.LARGE -> if (orientation == Orientation.PORTRAIT) 4 else 5
            ScreenSize.EXTRA_LARGE -> if (orientation == Orientation.PORTRAIT) 5 else 6
        }
    }
    
    /**
     * Get responsive icon size
     */
    @Composable
    fun getIconSize(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 20.dp
            ScreenSize.MEDIUM -> 24.dp
            ScreenSize.LARGE -> 28.dp
            ScreenSize.EXTRA_LARGE -> 32.dp
        }
    }
    
    /**
     * Get responsive button height
     */
    @Composable
    fun getButtonHeight(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 44.dp
            ScreenSize.MEDIUM -> 48.dp
            ScreenSize.LARGE -> 52.dp
            ScreenSize.EXTRA_LARGE -> 56.dp
        }
    }
    
    /**
     * Check if device is in compact mode (small screen)
     */
    @Composable
    fun isCompactScreen(): Boolean {
        return getScreenSize() == ScreenSize.SMALL
    }
    
    /**
     * Check if device is in expanded mode (large screen)
     */
    @Composable
    fun isExpandedScreen(): Boolean {
        return getScreenSize() in listOf(ScreenSize.LARGE, ScreenSize.EXTRA_LARGE)
    }
    
    /**
     * Get responsive spacing between elements
     */
    @Composable
    fun getSpacing(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 8.dp
            ScreenSize.MEDIUM -> 12.dp
            ScreenSize.LARGE -> 16.dp
            ScreenSize.EXTRA_LARGE -> 20.dp
        }
    }
    
    /**
     * Get screen width in DP
     */
    @Composable
    fun getScreenWidthDp(): Int {
        return LocalConfiguration.current.screenWidthDp
    }
    
    /**
     * Get screen height in DP
     */
    @Composable
    fun getScreenHeightDp(): Int {
        return LocalConfiguration.current.screenHeightDp
    }
    
    /**
     * Convert DP to PX
     */
    @Composable
    fun dpToPx(dp: Dp): Float {
        return with(LocalDensity.current) { dp.toPx() }
    }
    
    /**
     * Convert PX to DP
     */
    @Composable
    fun pxToDp(px: Float): Dp {
        return with(LocalDensity.current) { px.toDp() }
    }
}
