package com.example.jiva.utils

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.*
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

/**
 * Optimized API service that consolidates duplicate endpoints from JivaApiService.kt
 * This replaces redundant API calls and provides a cleaner interface
 * 
 * REMOVED DUPLICATES:
 * - Consolidated individual GET endpoints with syncAllData()
 * - Unified image upload endpoints
 * - Removed redundant filtered endpoints that duplicate main ones
 */
interface OptimizedApiService {
    
    // ============ AUTHENTICATION ============
    
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Login")
    suspend fun login(@Body request: ApiLoginRequest): ApiLoginResponse

    // ============ BULK DATA SYNC ============
    
    /**
     * Single endpoint for all data synchronization
     * Replaces individual GET endpoints: getUsers, getAccounts, getClosingBalances, 
     * getStocks, getSalePurchases, getLedgers, getExpiries, getTemplates, getPriceScreenData
     */
    @GET("api/sync-all-data")
    suspend fun syncAllData(): SyncDataResponse
    
    // ============ FILTERED DATA ENDPOINTS ============
    // Only keep essential filtered endpoints that provide significant performance benefits
    
    @GET("api/sale-purchase")
    suspend fun getSalePurchasesByDateRange(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("cmpCode") cmpCode: Int
    ): List<SalePurchaseEntity>
    
    @GET("api/ledgers")
    suspend fun getLedgersByAccount(@Query("acId") acId: Int): List<LedgerEntity>

    // ============ BUSINESS LOGIC ENDPOINTS ============
    
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/CompanyInfo")
    suspend fun getCompanyInfo(@Body request: CompanyInfoRequest): CompanyInfoResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/MsgTemplates")
    suspend fun getMsgTemplates(@Body request: MsgTemplatesRequest): MsgTemplatesResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/GetYear")
    suspend fun getYear(@Body request: YearRequest): YearResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/OutStanding")
    suspend fun getOutstanding(@Body request: OutstandingRequest): OutstandingResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Account_Names")
    suspend fun getAccountNames(@Body request: AccountNamesRequest): AccountNamesResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Stock")
    suspend fun getStock(@Body request: StockRequest): StockResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Accounts")
    suspend fun getAccountsFiltered(@Body request: AccountsRequest): AccountsResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/ledger")
    suspend fun getLedger(@Body request: LedgerRequest): LedgerResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/SalePurchase")
    suspend fun getSalePurchase(@Body request: SalePurchaseRequest): SalePurchaseResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Expiry")
    suspend fun getExpiry(@Body request: ExpiryRequest): ExpiryResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/PriceList")
    suspend fun getPriceList(@Body request: PriceListRequest): PriceListResponse

    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/DayEndInfo")
    suspend fun getDayEndInfo(@Body request: DayEndInfoRequest): DayEndInfoResponse

    // ============ IMAGE UPLOAD (UNIFIED) ============
    
    /**
     * Unified image upload endpoint
     * Replaces both uploadImage() and uploadImageJivaBusiness()
     * Uses the more standard JivaBusiness endpoint
     */
    @Multipart
    @POST("api/JivaBusiness/UploadImage")
    suspend fun uploadImage(@Part file: MultipartBody.Part): ImageUploadResponse
}

/**
 * Migration guide for replacing JivaApiService with OptimizedApiService:
 * 
 * OLD -> NEW:
 * - getUsers() -> Use syncAllData() and extract users from response
 * - getAccounts() -> Use syncAllData() and extract accounts from response
 * - getClosingBalances() -> Use syncAllData() and extract closing balances from response
 * - getStocks() -> Use syncAllData() and extract stocks from response
 * - getSalePurchases() -> Use syncAllData() and extract sale purchases from response
 * - getLedgers() -> Use syncAllData() and extract ledgers from response
 * - getExpiries() -> Use syncAllData() and extract expiries from response
 * - getTemplates() -> Use syncAllData() and extract templates from response
 * - getPriceScreenData() -> Use syncAllData() and extract price data from response
 * - uploadImage() OR uploadImageJivaBusiness() -> Use unified uploadImage()
 * 
 * REMOVED (redundant with main endpoints):
 * - getAccountsByCompany() -> Use syncAllData() with client-side filtering
 * - getClosingBalancesByCompany() -> Use syncAllData() with client-side filtering
 * - getStocksByCompany() -> Use syncAllData() with client-side filtering
 * - getSalePurchasesByCompany() -> Use syncAllData() with client-side filtering
 * - getExpiriesByCompany() -> Use syncAllData() with client-side filtering
 */
