package com.example.jiva

import android.app.Application
import androidx.multidex.MultiDexApplication
import com.example.jiva.utils.DeviceCompatibility
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class JivaApplication : MultiDexApplication() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
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
        
        Timber.d("JIVA Application started")
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
