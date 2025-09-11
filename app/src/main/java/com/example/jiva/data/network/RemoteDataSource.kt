package com.example.jiva.data.network

import com.example.jiva.data.database.entities.*
import com.example.jiva.data.api.models.SyncDataResponse
import com.example.jiva.data.api.models.LedgerRequest
import com.example.jiva.data.api.models.LedgerResponse
import com.example.jiva.data.api.models.SalePurchaseRequest
import com.example.jiva.data.api.models.SalePurchaseResponse
import com.example.jiva.data.api.models.ExpiryRequest
import com.example.jiva.data.api.models.ExpiryResponse
import com.example.jiva.data.api.models.PriceListRequest
import com.example.jiva.data.api.models.PriceListResponse
import com.example.jiva.data.api.models.ImageUploadResponse
import timber.log.Timber
import java.io.IOException
import android.net.Uri
import okhttp3.MultipartBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

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
    suspend fun getOutstanding(userId: Int, yearString: String, filters: Map<String, String>? = null): Result<com.example.jiva.data.api.models.OutstandingResponse> {
        return try {
            Timber.d("Making Outstanding API call with userId: $userId, yearString: $yearString, filters: $filters")
            val request = com.example.jiva.data.api.models.OutstandingRequest(userID = userId, yearString = yearString, filters = filters)
            Timber.d("Request body: $request")

            val response = apiService.getOutstanding(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Outstanding API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** MsgTemplates API */
    suspend fun getMsgTemplates(userId: Int): Result<com.example.jiva.data.api.models.MsgTemplatesResponse> {
        return try {
            Timber.d("Making MsgTemplates API call with userId: $userId")
            val request = com.example.jiva.data.api.models.MsgTemplatesRequest(userId)
            val response = apiService.getMsgTemplates(request)
            Timber.d("MsgTemplates response: isSuccess=${response.isSuccess}, size=${response.data?.size}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "MsgTemplates API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** CompanyInfo API */
    suspend fun getCompanyInfo(userId: Int): Result<com.example.jiva.data.api.models.CompanyInfoResponse> {
        return try {
            Timber.d("Making CompanyInfo API call with userId: $userId")
            val request = com.example.jiva.data.api.models.CompanyInfoRequest(userId)
            val response = apiService.getCompanyInfo(request)
            Timber.d("CompanyInfo response: isSuccess=${response.isSuccess}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "CompanyInfo API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Account Names API */
    suspend fun getAccountNames(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.AccountNamesResponse> {
        return try {
            Timber.d("Making Account_Names API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.AccountNamesRequest(userID = userId, yearString = yearString)
            val response = apiService.getAccountNames(request)
            Timber.d("Account_Names response: isSuccess=${response.isSuccess}, size=${response.data?.size}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Account_Names API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** Stock API */
    suspend fun getStock(userId: Int, yearString: String, filters: Map<String, String>? = emptyMap()): Result<com.example.jiva.data.api.models.StockResponse> {
        return try {
            Timber.d("Making Stock API call with userId: $userId, yearString: $yearString, filters: $filters")
            val request = com.example.jiva.data.api.models.StockRequest(userID = userId, yearString = yearString, filters = filters)
            Timber.d("Request body: $request")

            val response = apiService.getStock(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Stock API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLedger(userId: Int, yearString: String, filters: Map<String, String>? = null): Result<com.example.jiva.data.api.models.LedgerResponse> {
        return try {
            Timber.d("Making Ledger API call with userId: $userId, yearString: $yearString, filters: $filters")

            // Extract aC_ID from filters (backwards-compat) but send filters as empty object per API contract
            val accountId = filters?.get("aC_ID") ?: ""

            val request = com.example.jiva.data.api.models.LedgerRequest(
                userID = userId,
                yearString = yearString,
                aC_ID = accountId,
                filters = emptyMap()
            )
            Timber.d("Request body: $request")

            val response = apiService.getLedger(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Ledger API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getAccountsFiltered(userId: Int, yearString: String, filters: Map<String, String>): Result<com.example.jiva.data.api.models.AccountsResponse> {
        return try {
            Timber.d("Making Accounts API call with userId: $userId, yearString: $yearString, filters: $filters")
            val request = com.example.jiva.data.api.models.AccountsRequest(userID = userId, yearString = yearString, filters = filters)
            val response = apiService.getAccountsFiltered(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Accounts API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getSalePurchase(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.SalePurchaseResponse> {
        return try {
            Timber.d("Making SalePurchase API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.SalePurchaseRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getSalePurchase(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "SalePurchase API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getExpiry(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.ExpiryResponse> {
        return try {
            Timber.d("Making Expiry API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.ExpiryRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getExpiry(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Expiry API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getPriceList(userId: Int, yearString: String): Result<com.example.jiva.data.api.models.PriceListResponse> {
        return try {
            Timber.d("Making PriceList API call with userId: $userId, yearString: $yearString")
            val request = com.example.jiva.data.api.models.PriceListRequest(userID = userId, yearString = yearString)
            Timber.d("Request body: $request")

            val response = apiService.getPriceList(request)
            Timber.d("API response received: isSuccess=${response.isSuccess}, message=${response.message}, data size=${response.data?.size}")

            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "PriceList API call failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Fetch financial year list for the user
     */
    suspend fun getYears(userId: Int): Result<com.example.jiva.data.api.models.YearResponse> {
        return try {
            val request = com.example.jiva.data.api.models.YearRequest(userId)
            val response = apiService.getYear(request)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "GetYear API call failed: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Get all data in a single call (for initial sync)
     */
    suspend fun syncAllData(): Result<SyncDataResponse> {
        return safeApiCall { apiService.syncAllData() }
    }
    
    /**
     * Upload image and get URL back
     * 
     * API Structure for Image Upload:
     * - Endpoint: POST /api/upload-image
     * - Content-Type: multipart/form-data
     * - Body: image file (MultipartBody.Part)
     * - Response: ImageUploadResponse
     * 
     * Note: This is a simplified implementation. In production, you would:
     * 1. Use ContentResolver to get the actual file from URI
     * 2. Handle different image formats (JPEG, PNG, etc.)
     * 3. Add proper error handling for file operations
     * 4. Implement actual API call instead of mock response
     */
    suspend fun uploadImage(imageUri: Uri): Result<ImageUploadResponse> {
        return try {
            // Convert URI to temp file and upload via Retrofit multipart
            val ctx = com.example.jiva.JivaApplication.getInstance().applicationContext
            val contentResolver = ctx.contentResolver
            val fileName = "upload_${System.currentTimeMillis()}.jpg"
            val tempFile = File(ctx.cacheDir, fileName)
            contentResolver.openInputStream(imageUri)?.use { input: InputStream ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IOException("Failed to open input stream for URI")

            // OkHttp 4.x: use extension functions instead of deprecated parse/create
            val mediaType = "image/*".toMediaType()
            val requestFile = tempFile.asRequestBody(mediaType)
            val part = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)

            val response = apiService.uploadImageJivaBusiness(part)
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "Image upload failed: ${e.message}")
            Result.failure(e)
        }
    }

    /** DayEndInfo API */
    suspend fun getDayEndInfo(cmpCode: Int, dayDate: String): Result<com.example.jiva.data.api.models.DayEndInfoResponse> {
        return try {
            Timber.d("Making DayEndInfo API call with cmpCode: $cmpCode, dayDate: $dayDate")
            val request = com.example.jiva.data.api.models.DayEndInfoRequest(cmpCode = cmpCode, dayDate = dayDate)
            val response = apiService.getDayEndInfo(request)
            Timber.d("DayEndInfo response: isSuccess=${response.isSuccess}")
            Result.success(response)
        } catch (e: Exception) {
            Timber.e(e, "DayEndInfo API call failed: ${e.message}")
            Result.failure(e)
        }
    }
}