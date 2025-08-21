package com.example.jiva.data.api

import com.example.jiva.data.database.entities.*
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit API service interface for JIVA server endpoints
 * Maps to your custom server API endpoints
 */
interface JivaApiService {
    
    /**
     * GET /api/users
     * Fetch all users from server
     */
    @GET("api/users")
    suspend fun getUsers(): List<UserEntity>
    
    /**
     * GET /api/accounts
     * Fetch all account master data from server
     */
    @GET("api/accounts")
    suspend fun getAccounts(): List<AccountMasterEntity>
    
    /**
     * GET /api/closing-balances
     * Fetch all closing balance data for outstanding reports
     */
    @GET("api/closing-balances")
    suspend fun getClosingBalances(): List<ClosingBalanceEntity>
    
    /**
     * GET /api/stocks
     * Fetch all stock data from server
     */
    @GET("api/stocks")
    suspend fun getStocks(): List<StockEntity>
    
    /**
     * GET /api/sale-purchase
     * Fetch all sale/purchase transaction data
     */
    @GET("api/sale-purchase")
    suspend fun getSalePurchases(): List<SalePurchaseEntity>
    
    /**
     * GET /api/ledgers
     * Fetch all ledger entries from server
     */
    @GET("api/ledgers")
    suspend fun getLedgers(): List<LedgerEntity>
    
    /**
     * GET /api/expiries
     * Fetch all expiry data from server
     */
    @GET("api/expiries")
    suspend fun getExpiries(): List<ExpiryEntity>
    
    /**
     * GET /api/templates
     * Fetch all WhatsApp message templates
     */
    @GET("api/templates")
    suspend fun getTemplates(): List<TemplateEntity>
    
    /**
     * GET /api/priceScreen
     * Fetch price screen data (you may need to define specific data model)
     */
    @GET("api/priceScreen")
    suspend fun getPriceScreenData(): List<Any> // Replace with specific price data model
    
    // Additional filtered endpoints for better performance
    
    @GET("api/accounts")
    suspend fun getAccountsByCompany(@Query("cmpCode") cmpCode: Int): List<AccountMasterEntity>
    
    @GET("api/closing-balances")
    suspend fun getClosingBalancesByCompany(@Query("cmpCode") cmpCode: Int): List<ClosingBalanceEntity>
    
    @GET("api/stocks")
    suspend fun getStocksByCompany(@Query("cmpCode") cmpCode: Int): List<StockEntity>
    
    @GET("api/sale-purchase")
    suspend fun getSalePurchasesByCompany(@Query("cmpCode") cmpCode: Int): List<SalePurchaseEntity>
    
    @GET("api/sale-purchase")
    suspend fun getSalePurchasesByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("cmpCode") cmpCode: Int
    ): List<SalePurchaseEntity>
    
    @GET("api/ledgers")
    suspend fun getLedgersByAccount(@Query("acId") acId: Int): List<LedgerEntity>
    
    @GET("api/expiries")
    suspend fun getExpiriesByCompany(@Query("cmpCode") cmpCode: Int): List<ExpiryEntity>
}
