package com.example.jiva.data.network

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.SyncDataResponse
import com.example.jiva.data.api.models.LedgerRequest
import com.example.jiva.data.api.models.LedgerResponse
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
            val body = apiCall()
            Result.success(body)
        } catch (e: Exception) {
            Timber.e(e, "Exception during API call: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get all users from API
     */
    suspend fun getUsers(): Result<List<UserEntity>> {
        return  safeApiCall { apiService.getUsers() }
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
    
    /** Outstanding API */
    suspend fun getOutstanding(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.OutstandingResponse> {
        return try {
            Timber.d("Making Outstanding API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.OutstandingRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getOutstanding(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Outstanding API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Stock API */
    suspend fun getStock(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.StockResponse> {
        return try {
            Timber.d("Making Stock API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.StockRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getStock(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Stock API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLedger(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.LedgerResponse> {
        return try {
            Timber.d("Making Ledger API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.LedgerRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getLedger(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Ledger API call failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get all data in a single call (for initial sync)
     */
    suspend fun syncAllData(): Result<SyncDataResponse> {
        return safeApiCall { apiService.syncAllData() }
    }
}