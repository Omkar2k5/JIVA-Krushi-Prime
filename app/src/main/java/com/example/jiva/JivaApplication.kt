package com.example.jiva

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.jiva.data.database.DummyDataProvider
import com.example.jiva.data.database.JivaDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Custom Application class for JIVA Business Management System
 * Optimized for performance and compatibility across Android 7-15
 */
class JivaApplication : MultiDexApplication() {

    // Database instance accessible throughout the app
    val database by lazy { JivaDatabase.getDatabase(this) }
    
    // Application scope for coroutines
    private val applicationScope = CoroutineScope(Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber for logging
        initializeLogging()

        // Performance optimizations
        initializePerformanceOptimizations()
        
        // Initialize database with dummy data
        initializeDatabase()

        Timber.d("JIVA Application started successfully")

        // Log basic device information
        logDeviceInformation()
    }
    
    /**
     * Initialize the database with dummy data
     */
    private fun initializeDatabase() {
        applicationScope.launch {
            try {
                Timber.d("Populating database with dummy data")
                DummyDataProvider.populateDatabase(database)
                Timber.d("Database populated successfully")
            } catch (e: Exception) {
                Timber.e(e, "Error populating database with dummy data")
            }
        }
    }

    private fun initializeLogging() {
        try {
            // Check if we're in debug mode using application info
            val isDebuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
            if (isDebuggable) {
                Timber.plant(Timber.DebugTree())
            } else {
                // In production, you might want to use a different tree
                // that logs to crash reporting services like Firebase Crashlytics
                Timber.plant(ProductionTree())
            }
        } catch (e: Exception) {
            // Fallback to debug tree if there's any issue
            Timber.plant(Timber.DebugTree())
        }
    }

    private fun initializePerformanceOptimizations() {
        // Enable strict mode in debug builds for performance monitoring
        if (BuildConfig.DEBUG) {
            enableStrictMode()
        }

        // Pre-load critical resources
        preloadCriticalResources()
    }

    private fun logDeviceInformation() {
        Timber.d("Device Info: ${android.os.Build.MODEL} (${android.os.Build.MANUFACTURER})")
        Timber.d("Android Version: ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})")
        Timber.d("Screen density: ${resources.displayMetrics.densityDpi}")
        Timber.d("Available memory: ${getAvailableMemory()} MB")
    }
    
    private fun enableStrictMode() {
        // StrictMode helps detect performance issues during development
        try {
            android.os.StrictMode.setThreadPolicy(
                android.os.StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )

            android.os.StrictMode.setVmPolicy(
                android.os.StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to enable StrictMode")
        }
    }

    private fun preloadCriticalResources() {
        // Pre-load commonly used resources to improve performance
        try {
            // Pre-load colors
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                resources.getColor(android.R.color.white, theme)
                resources.getColor(android.R.color.black, theme)
            } else {
                @Suppress("DEPRECATION")
                resources.getColor(android.R.color.white)
                @Suppress("DEPRECATION")
                resources.getColor(android.R.color.black)
            }

            // Pre-load common drawables
            resources.getDrawable(R.drawable.logo, theme)

            Timber.d("Critical resources preloaded")
        } catch (e: Exception) {
            Timber.w(e, "Failed to preload some resources")
        }
    }

    private fun getAvailableMemory(): Long {
        val activityManager = getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memoryInfo = android.app.ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024) // Convert to MB
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Timber.w("Low memory warning received")
        // Perform memory cleanup if needed
        System.gc()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Timber.d("Memory trim requested with level: $level")

        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE,
            TRIM_MEMORY_RUNNING_LOW,
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                // App is running but system is low on memory
                performMemoryCleanup()
            }
            TRIM_MEMORY_UI_HIDDEN -> {
                // App UI is hidden, release UI-related resources
                performUIMemoryCleanup()
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_COMPLETE -> {
                // App is in background, release as much memory as possible
                performAggressiveMemoryCleanup()
            }
        }
    }

    private fun performMemoryCleanup() {
        Timber.d("Performing memory cleanup")
        System.gc()
    }

    private fun performUIMemoryCleanup() {
        Timber.d("Performing UI memory cleanup")
        System.gc()
    }

    private fun performAggressiveMemoryCleanup() {
        Timber.d("Performing aggressive memory cleanup")
        System.gc()
    }

    /**
     * Production logging tree that filters out debug logs
     * and can be extended to send logs to crash reporting services
     */
    private class ProductionTree : Timber.Tree() {
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Only log warnings, errors, and info in production
            if (priority >= android.util.Log.INFO) {
                // Here you could send logs to your crash reporting service
                // Example: Crashlytics.log(message)
                android.util.Log.println(priority, tag, message)
            }
        }
    }
}
