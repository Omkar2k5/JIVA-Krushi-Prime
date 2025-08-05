package com.example.jiva

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class JivaApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Timber for logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, you might want to use a different tree
            // that logs to crash reporting services like Firebase Crashlytics
            Timber.plant(ProductionTree())
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
