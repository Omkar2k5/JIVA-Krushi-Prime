package com.example.jiva.data.api

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.SyncDataResponse
import com.example.jiva.data.api.models.ApiLoginRequest
import com.example.jiva.data.api.models.ApiLoginResponse
import com.example.jiva.data.api.models.CompanyInfoRequest
import com.example.jiva.data.api.models.CompanyInfoResponse
import com.example.jiva.data.api.models.OutstandingRequest
import com.example.jiva.data.api.models.OutstandingResponse
import com.example.jiva.data.api.models.StockRequest
import com.example.jiva.data.api.models.StockResponse
import com.example.jiva.data.api.models.LedgerRequest
import com.example.jiva.data.api.models.LedgerResponse
import com.example.jiva.data.api.models.SalePurchaseRequest
import com.example.jiva.data.api.models.SalePurchaseResponse
import com.example.jiva.data.api.models.ExpiryRequest
import com.example.jiva.data.api.models.ExpiryResponse
import com.example.jiva.data.api.models.PriceListRequest
import com.example.jiva.data.api.models.PriceListResponse
import com.example.jiva.data.api.models.ImageUploadResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.MultipartBody

/**
 * Retrofit API service interface for JIVA server endpoints
 * Maps to your custom server API endpoints
 */
interface JivaApiService {
    
    /**
     * POST /api/JivaBusiness/Login
     * Login with mobile number and password
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Login")
    suspend fun login(@Body request: ApiLoginRequest): ApiLoginResponse

    /**
     * GET /api/sync-all-data
     * Fetch all data from server in a single request for local database sync
     * This is the main endpoint for syncing all data according to API_DATA_REQUIREMENTS.txt
     */
    @GET("api/sync-all-data")
    suspend fun syncAllData(): SyncDataResponse
    
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
     * Fetch price screen data for price management functionality
     */
    @GET("api/priceScreen")
    suspend fun getPriceScreenData(): List<PriceDataEntity>

    /**
     * POST /api/JivaBusiness/CompanyInfo
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/CompanyInfo")
    suspend fun getCompanyInfo(@Body request: CompanyInfoRequest): CompanyInfoResponse

    /**
     * POST /api/JivaBusiness/MsgTemplates
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/MsgTemplates")
    suspend fun getMsgTemplates(@Body request: com.example.jiva.data.api.models.MsgTemplatesRequest): com.example.jiva.data.api.models.MsgTemplatesResponse

    /**
     * POST /api/JivaBusiness/GetYear
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/GetYear")
    suspend fun getYear(@Body request: com.example.jiva.data.api.models.YearRequest): com.example.jiva.data.api.models.YearResponse

    /**
     * POST /api/JivaBusiness/OutStanding
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/OutStanding")
    suspend fun getOutstanding(@Body request: OutstandingRequest): OutstandingResponse

    /**
     * POST /api/JivaBusiness/Account_Names
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Account_Names")
    suspend fun getAccountNames(@Body request: com.example.jiva.data.api.models.AccountNamesRequest): com.example.jiva.data.api.models.AccountNamesResponse

    /**
     * POST /api/JivaBusiness/Stock
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Stock")
    suspend fun getStock(@Body request: StockRequest): StockResponse

    /**
     * POST /api/JivaBusiness/Accounts (for opening balance & CR/DR)
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Accounts")
    suspend fun getAccountsFiltered(@Body request: com.example.jiva.data.api.models.AccountsRequest): com.example.jiva.data.api.models.AccountsResponse

    /**
     * POST /api/JivaBusiness/ledger
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/ledger")
    suspend fun getLedger(@Body request: LedgerRequest): LedgerResponse

    /**
     * POST /api/JivaBusiness/SalePurchase
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/SalePurchase")
    suspend fun getSalePurchase(@Body request: SalePurchaseRequest): SalePurchaseResponse

    /**
     * POST /api/JivaBusiness/Expiry
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/Expiry")
    suspend fun getExpiry(@Body request: ExpiryRequest): ExpiryResponse

    /**
     * POST /api/JivaBusiness/PriceList
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/PriceList")
    suspend fun getPriceList(@Body request: PriceListRequest): PriceListResponse

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
    
    /**
     * POST /api/upload-image
     * Upload image file and get URL back
     */
    @Multipart
    @POST("api/upload-image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): ImageUploadResponse

    /**
     * POST /api/JivaBusiness/UploadImage
     * Upload image file (field name: file) and get imageUrl back
     */
    @Multipart
    @POST("api/JivaBusiness/UploadImage")
    suspend fun uploadImageJivaBusiness(@Part file: MultipartBody.Part): ImageUploadResponse

    /**
     * POST /api/JivaBusiness/DayEndInfo
     */
    @Headers("Content-Type: application/json")
    @POST("api/JivaBusiness/DayEndInfo")
    suspend fun getDayEndInfo(@Body request: com.example.jiva.data.api.models.DayEndInfoRequest): com.example.jiva.data.api.models.DayEndInfoResponse
}
