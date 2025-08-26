package com.example.jiva.data.network

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.SyncDataResponse
import timber.log.Timber
import java.io.IOException

/**
 * Remote data source for API calls
 * Handles all network operations and error handling
 */
class RemoteDataSource {
    
    private val apiService = RetrofitClient.jivaApiService
    
    /**
     * Generic function to make API calls with error handling
     */
    private suspend fun <T> safeApiCall(apiCall: suspend () -> T): Result<T> {
        return try {
            // Simulate API call failure for development. Comment this return to enable real calls.
            Timber.d("Simulating API call failure for development")
            return Result.failure(IOException("Server not available - using dummy data"))
            
            // For real API calls, use:
            // val body = apiCall()
            // Result.success(body)
        } catch (e: Exception) {
            Timber.e(e, "Exception during API call: ${e.message}")
            Result.failure(IOException("Server not available - using dummy data", e))
        }
    }
    
    /**
     * Get all users from API
     */
    suspend fun getUsers(): Result<List<UserEntity>> {
        return safeApiCall { apiService.getUsers() }
    }
    
    /**
     * Get all accounts from API
     */
    suspend fun getAccounts(): Result<List<AccountMasterEntity>> {
        return safeApiCall { apiService.getAccounts() }
    }
    
    /**
     * Get all closing balances from API
     */
    suspend fun getClosingBalances(): Result<List<ClosingBalanceEntity>> {
        return safeApiCall { apiService.getClosingBalances() }
    }
    
    /**
     * Get all stocks from API
     */
    suspend fun getStocks(): Result<List<StockEntity>> {
        return safeApiCall { apiService.getStocks() }
    }
    
    /**
     * Get all sale/purchase records from API
     */
    suspend fun getSalePurchases(): Result<List<SalePurchaseEntity>> {
        return safeApiCall { apiService.getSalePurchases() }
    }
    
    /**
     * Get all ledger entries from API
     */
    suspend fun getLedgers(): Result<List<LedgerEntity>> {
        return safeApiCall { apiService.getLedgers() }
    }
    
    /**
     * Get all expiry items from API
     */
    suspend fun getExpiries(): Result<List<ExpiryEntity>> {
        return safeApiCall { apiService.getExpiries() }
    }
    
    /**
     * Get all templates from API
     */
    suspend fun getTemplates(): Result<List<TemplateEntity>> {
        return safeApiCall { apiService.getTemplates() }
    }
    
    /**
     * Get price screen data from API
     */
    suspend fun getPriceScreenData(): Result<List<PriceDataEntity>> {
        return safeApiCall { apiService.getPriceScreenData() }
    }
    
    /**
     * Get all data in a single call (for initial sync)
     */
    suspend fun syncAllData(): Result<SyncDataResponse> {
        return safeApiCall { apiService.syncAllData() }
    }
}