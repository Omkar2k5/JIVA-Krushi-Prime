package com.example.jiva.data.network

import android.content.Context
import com.example.jiva.BuildConfig
import com.example.jiva.data.api.JivaApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Dynamic Retrofit client for API calls
 * Supports runtime IP configuration from storage
 */
object RetrofitClient {
    
    // Default fallback URL
    private const val DEFAULT_BASE_URL = "http://103.48.42.125:8081/"
    
    // Current base URL (can be updated dynamically)
    @Volatile
    private var currentBaseUrl: String = DEFAULT_BASE_URL
    
    // Create OkHttpClient with logging and timeouts
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        })
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    // Retrofit instance (recreated when base URL changes)
    @Volatile
    private var retrofit: Retrofit = createRetrofit(currentBaseUrl)
    
    // API service instance
    @Volatile
    private var apiService: JivaApiService = retrofit.create(JivaApiService::class.java)
    
    /**
     * Get the API service instance
     * This is the main entry point for all API calls
     */
    val jivaApiService: JivaApiService
        get() = apiService
    
    /**
     * Initialize with stored IP from context
     * Call this on app startup
     */
    fun initialize(context: Context) {
        val storedIp = com.example.jiva.utils.UserEnv.getServerIp(context)
        if (storedIp != null && storedIp.isNotBlank()) {
            val baseUrl = formatBaseUrl(storedIp)
            updateBaseUrl(baseUrl)
            timber.log.Timber.d("✅ RetrofitClient initialized with stored IP: $baseUrl")
        } else {
            timber.log.Timber.d("ℹ️ RetrofitClient using default IP: $DEFAULT_BASE_URL")
        }
    }
    
    /**
     * Update the base URL and recreate Retrofit instance
     * This allows dynamic server switching without app restart
     */
    @Synchronized
    fun updateBaseUrl(newBaseUrl: String) {
        val formattedUrl = formatBaseUrl(newBaseUrl)
        if (formattedUrl != currentBaseUrl) {
            currentBaseUrl = formattedUrl
            retrofit = createRetrofit(currentBaseUrl)
            apiService = retrofit.create(JivaApiService::class.java)
            timber.log.Timber.d("🔄 RetrofitClient base URL updated to: $currentBaseUrl")
        }
    }
    
    /**
     * Get current base URL
     */
    fun getCurrentBaseUrl(): String = currentBaseUrl
    
    /**
     * Create a new Retrofit instance with the given base URL
     */
    private fun createRetrofit(baseUrl: String): Retrofit {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
    
    /**
     * Format IP to proper base URL format
     * Handles:
     * - "103.48.42.125:8081" -> "http://103.48.42.125:8081/"
     * - "http://103.48.42.125:8081" -> "http://103.48.42.125:8081/"
     * - "103.48.42.125:8081/" -> "http://103.48.42.125:8081/"
     */
    private fun formatBaseUrl(ip: String): String {
        var url = ip.trim()
        
        // Add http:// if not present
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        
        // Add trailing slash if not present
        if (!url.endsWith("/")) {
            url = "$url/"
        }
        
        return url
    }
}
