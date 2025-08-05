package com.example.jiva.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import timber.log.Timber

/**
 * Utility class for handling device compatibility across different Android versions,
 * screen sizes, and densities. Essential for supporting 100+ users with varying devices.
 */
object DeviceCompatibility {
    
    /**
     * Device screen size categories
     */
    enum class ScreenSize {
        SMALL,      // < 600dp width
        MEDIUM,     // 600dp - 840dp width  
        LARGE,      // > 840dp width
        EXTRA_LARGE // > 1200dp width
    }
    
    /**
     * Device type categories
     */
    enum class DeviceType {
        PHONE,
        TABLET,
        FOLDABLE,
        DESKTOP
    }
    
    /**
     * Get screen size category based on width
     */
    @Composable
    fun getScreenSize(): ScreenSize {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        
        return when {
            screenWidthDp < 600.dp -> ScreenSize.SMALL
            screenWidthDp < 840.dp -> ScreenSize.MEDIUM
            screenWidthDp < 1200.dp -> ScreenSize.LARGE
            else -> ScreenSize.EXTRA_LARGE
        }
    }
    
    /**
     * Get device type based on screen characteristics
     */
    @Composable
    fun getDeviceType(): DeviceType {
        val configuration = LocalConfiguration.current
        val screenWidthDp = configuration.screenWidthDp.dp
        val screenHeightDp = configuration.screenHeightDp.dp
        
        return when {
            // Foldable detection (approximate)
            screenWidthDp > 800.dp && screenHeightDp < 600.dp -> DeviceType.FOLDABLE
            // Desktop/Chromebook detection
            screenWidthDp > 1200.dp -> DeviceType.DESKTOP
            // Tablet detection
            screenWidthDp >= 600.dp -> DeviceType.TABLET
            // Default to phone
            else -> DeviceType.PHONE
        }
    }
    
    /**
     * Check if device is in landscape mode
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }
    
    /**
     * Get appropriate padding based on screen size
     */
    @Composable
    fun getScreenPadding(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> 16.dp
            ScreenSize.MEDIUM -> 24.dp
            ScreenSize.LARGE -> 32.dp
            ScreenSize.EXTRA_LARGE -> 48.dp
        }
    }
    
    /**
     * Get appropriate content width based on screen size
     */
    @Composable
    fun getContentMaxWidth(): Dp {
        return when (getScreenSize()) {
            ScreenSize.SMALL -> Dp.Unspecified
            ScreenSize.MEDIUM -> 600.dp
            ScreenSize.LARGE -> 800.dp
            ScreenSize.EXTRA_LARGE -> 1000.dp
        }
    }
    
    /**
     * Check Android version compatibility
     */
    fun isAndroidVersionSupported(minVersion: Int): Boolean {
        return Build.VERSION.SDK_INT >= minVersion
    }
    
    /**
     * Get device information for logging/debugging
     */
    fun getDeviceInfo(context: Context): String {
        val displayMetrics = context.resources.displayMetrics
        
        return buildString {
            appendLine("Device Info:")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- Manufacturer: ${Build.MANUFACTURER}")
            appendLine("- Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("- Screen Density: ${displayMetrics.densityDpi} dpi")
            appendLine("- Screen Size: ${displayMetrics.widthPixels}x${displayMetrics.heightPixels} px")
            appendLine("- Density Scale: ${displayMetrics.density}")
        }
    }
    
    /**
     * Log device compatibility information
     */
    fun logDeviceCompatibility(context: Context) {
        val deviceInfo = getDeviceInfo(context)
        Timber.d(deviceInfo)
        
        // Check for potential compatibility issues
        when {
            Build.VERSION.SDK_INT < 24 -> {
                Timber.w("Device running Android API level ${Build.VERSION.SDK_INT} - below minimum supported (24)")
            }
            Build.VERSION.SDK_INT > 35 -> {
                Timber.i("Device running newer Android API level ${Build.VERSION.SDK_INT} - may need testing")
            }
        }
        
        val displayMetrics = context.resources.displayMetrics
        if (displayMetrics.densityDpi < 160) {
            Timber.w("Low density screen detected (${displayMetrics.densityDpi} dpi) - UI may need adjustments")
        }
    }
    
    /**
     * Check if device supports specific features
     */
    fun checkFeatureSupport(context: Context) {
        val packageManager = context.packageManager
        
        val features = mapOf(
            "Camera" to "android.hardware.camera",
            "Fingerprint" to "android.hardware.fingerprint",
            "NFC" to "android.hardware.nfc",
            "Bluetooth" to "android.hardware.bluetooth",
            "WiFi" to "android.hardware.wifi"
        )
        
        features.forEach { (name, feature) ->
            val supported = packageManager.hasSystemFeature(feature)
            Timber.d("Feature $name: ${if (supported) "Supported" else "Not supported"}")
        }
    }
    
    /**
     * Get recommended UI scale based on device characteristics
     */
    @Composable
    fun getUIScale(): Float {
        val density = LocalDensity.current.density
        val screenSize = getScreenSize()
        
        return when {
            density < 1.0f -> 1.1f // Increase scale for low density screens
            screenSize == ScreenSize.EXTRA_LARGE -> 1.2f // Increase scale for very large screens
            else -> 1.0f
        }
    }
    
    /**
     * Check if device is likely to have performance constraints
     */
    fun isLowEndDevice(): Boolean {
        return when {
            Build.VERSION.SDK_INT < 26 -> true // Android 8.0 and below
            Runtime.getRuntime().maxMemory() < 512 * 1024 * 1024 -> true // Less than 512MB heap
            else -> false
        }
    }
}
