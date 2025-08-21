package com.example.jiva.data.network

import com.example.jiva.data.database.entities.*
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service interface for JIVA application
 * Defines all the API endpoints for data synchronization
 */
interface JivaApiService {
    
    /**
     * Get all users
     * @return List of UserEntity objects
     */
    @GET("/api/users")
    suspend fun getUsers(): Response<List<UserEntity>>
    
    /**
     * Get all accounts
     * @return List of AccountMasterEntity objects
     */
    @GET("/api/accounts")
    suspend fun getAccounts(): Response<List<AccountMasterEntity>>
    
    /**
     * Get all closing balances
     * @return List of ClosingBalanceEntity objects
     */
    @GET("/api/closing-balances")
    suspend fun getClosingBalances(): Response<List<ClosingBalanceEntity>>
    
    /**
     * Get all stocks
     * @return List of StockEntity objects
     */
    @GET("/api/stocks")
    suspend fun getStocks(): Response<List<StockEntity>>
    
    /**
     * Get all sale/purchase records
     * @return List of SalePurchaseEntity objects
     */
    @GET("/api/sale-purchase")
    suspend fun getSalePurchases(): Response<List<SalePurchaseEntity>>
    
    /**
     * Get all ledger entries
     * @return List of LedgerEntity objects
     */
    @GET("/api/ledgers")
    suspend fun getLedgers(): Response<List<LedgerEntity>>
    
    /**
     * Get all expiry items
     * @return List of ExpiryEntity objects
     */
    @GET("/api/expiries")
    suspend fun getExpiries(): Response<List<ExpiryEntity>>
    
    /**
     * Get all templates
     * @return List of TemplateEntity objects
     */
    @GET("/api/templates")
    suspend fun getTemplates(): Response<List<TemplateEntity>>
    
    /**
     * Get price screen data
     * @return Generic price data (will need to be mapped to specific model)
     */
    @GET("/api/priceScreen")
    suspend fun getPriceScreenData(): Response<Map<String, Any>>
    
    /**
     * Get all data in a single call (for initial sync)
     * @return Map of all entity types
     */
    @GET("/api/all-data")
    suspend fun getAllData(): Response<Map<String, List<Any>>>
}