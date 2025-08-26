package com.example.jiva.utils

import android.content.Context
import timber.log.Timber

/**
 * Debug helper for Outstanding screen issues
 */
object OutstandingDebugHelper {
    
    /**
     * Set a test user ID for debugging Outstanding API calls
     */
    fun setTestUserId(context: Context, userId: String = "1017") {
        UserEnv.setUserId(context, userId)
        Timber.d("Set test userId: $userId")
    }
    
    /**
     * Set a test financial year for debugging
     */
    fun setTestFinancialYear(context: Context, year: String = "2025-26") {
        UserEnv.setFinancialYear(context, year)
        Timber.d("Set test financial year: $year")
    }
    
    /**
     * Check current user environment settings
     */
    fun checkUserEnvironment(context: Context) {
        val userId = UserEnv.getUserId(context)
        val year = UserEnv.getFinancialYear(context)
        val companyName = UserEnv.getCompanyName(context)
        
        Timber.d("Current User Environment:")
        Timber.d("  User ID: $userId")
        Timber.d("  Financial Year: $year")
        Timber.d("  Company Name: $companyName")
    }
    
    /**
     * Initialize test environment for Outstanding debugging
     */
    fun initializeTestEnvironment(context: Context) {
        setTestUserId(context, "1017")
        setTestFinancialYear(context, "2025-26")
        checkUserEnvironment(context)
    }
}
