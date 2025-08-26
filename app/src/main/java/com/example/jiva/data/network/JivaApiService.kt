package com.example.jiva.data.network

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.SyncDataResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Legacy duplicate interface returning Response<T> is no longer used.
 * Use com.example.jiva.data.api.JivaApiService instead.
 */
@Deprecated("Use com.example.jiva.data.api.JivaApiService")
interface JivaApiService {
    @GET("/api/users")
    suspend fun getUsers(): List<UserEntity>

    @GET("/api/accounts")
    suspend fun getAccounts(): List<AccountMasterEntity>

    @GET("/api/closing-balances")
    suspend fun getClosingBalances(): List<ClosingBalanceEntity>

    @GET("/api/stocks")
    suspend fun getStocks(): List<StockEntity>

    @GET("/api/sale-purchase")
    suspend fun getSalePurchases(): List<SalePurchaseEntity>

    @GET("/api/ledgers")
    suspend fun getLedgers(): List<LedgerEntity>

    @GET("/api/expiries")
    suspend fun getExpiries(): List<ExpiryEntity>

    @GET("/api/templates")
    suspend fun getTemplates(): List<TemplateEntity>

    @GET("/api/priceScreen")
    suspend fun getPriceScreenData(): List<PriceDataEntity>

    @GET("/api/sync-all-data")
    suspend fun syncAllData(): SyncDataResponse
}