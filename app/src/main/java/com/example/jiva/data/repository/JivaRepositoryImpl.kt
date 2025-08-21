package com.example.jiva.data.repository

import com.example.jiva.data.database.DummyDataProvider
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.*
import com.example.jiva.data.network.RemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import timber.log.Timber
import java.math.BigDecimal
import java.util.Date

/**
 * Implementation of JivaRepository that uses local database and remote API
 * Can work with both real API data and dummy data for development
 */
class JivaRepositoryImpl(
    private val database: JivaDatabase,
    private val remoteDataSource: RemoteDataSource = RemoteDataSource()
) : JivaRepository {
    
    // User operations
    override suspend fun getUsers(): List<UserEntity> {
        return try {
            database.userDao().getAllUsers().first()
        } catch (e: Exception) {
            Timber.e(e, "Error getting users from local database")
            emptyList()
        }
    }
    
    override suspend fun getUserById(userId: Int): UserEntity? {
        return database.userDao().getUserById(userId)
    }
    
    override suspend fun getUserByMobile(mobileNumber: String): UserEntity? {
        return database.userDao().getUserByMobile(mobileNumber)
    }
    
    override suspend fun syncUsers(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getUsers()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val users = apiResult.getOrNull()
                if (users != null) {
                    database.userDao().insertUsers(users)
                    Timber.d("Successfully loaded ${users.size} users from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy users: ${apiResult.exceptionOrNull()?.message}")
                database.userDao().insertUsers(DummyDataProvider.getDummyUsers())
                Timber.d("Successfully loaded dummy users as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing users")
            Result.failure(e)
        }
    }
    
    // Account Master operations
    override fun getAllAccounts(): Flow<List<AccountMasterEntity>> {
        return database.accountMasterDao().getAllAccounts()
    }
    
    override suspend fun getAccountByAcId(acId: Int): AccountMasterEntity? {
        return database.accountMasterDao().getAccountByAcId(acId)
    }
    
    override suspend fun searchAccountsByName(searchTerm: String): Flow<List<AccountMasterEntity>> {
        return database.accountMasterDao().searchAccountsByName(searchTerm)
    }
    
    override suspend fun getAllAreas(): List<String> {
        return database.accountMasterDao().getAllAreas()
    }
    
    override suspend fun syncAccounts(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getAccounts()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val accounts = apiResult.getOrNull()
                if (accounts != null) {
                    database.accountMasterDao().insertAccounts(accounts)
                    Timber.d("Successfully loaded ${accounts.size} accounts from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy accounts: ${apiResult.exceptionOrNull()?.message}")
                database.accountMasterDao().insertAccounts(DummyDataProvider.getDummyAccounts())
                Timber.d("Successfully loaded dummy accounts as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing accounts")
            Result.failure(e)
        }
    }
    
    // Closing Balance operations
    override fun getAllClosingBalances(): Flow<List<ClosingBalanceEntity>> {
        return database.closingBalanceDao().getAllClosingBalances()
    }
    
    override suspend fun getClosingBalanceByAcId(acId: Int): ClosingBalanceEntity? {
        return database.closingBalanceDao().getClosingBalanceByAcId(acId)
    }
    
    override suspend fun getTotalOutstanding(cmpCode: Int): Double? {
        return database.closingBalanceDao().getTotalOutstandingByCompany(cmpCode)
    }
    
    override suspend fun syncClosingBalances(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getClosingBalances()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val balances = apiResult.getOrNull()
                if (balances != null) {
                    database.closingBalanceDao().insertClosingBalances(balances)
                    Timber.d("Successfully loaded ${balances.size} closing balances from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy closing balances: ${apiResult.exceptionOrNull()?.message}")
                database.closingBalanceDao().insertClosingBalances(DummyDataProvider.getDummyClosingBalances())
                Timber.d("Successfully loaded dummy closing balances as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing closing balances")
            Result.failure(e)
        }
    }
    
    // Stock operations - Temporarily using empty implementations
    override fun getAllStocks(): Flow<List<StockEntity>> {
        return flowOf(emptyList())
    }
    
    override suspend fun getStockByItemId(itemId: Int): StockEntity? {
        return null
    }
    
    override suspend fun getStocksByItemType(itemType: String): Flow<List<StockEntity>> {
        return flowOf(emptyList())
    }
    
    override suspend fun getTotalStockValuation(cmpCode: Int): BigDecimal? {
        return BigDecimal.ZERO
    }
    
    override suspend fun getAllItemTypes(): List<String> {
        return emptyList()
    }
    
    override suspend fun getAllCompanies(): List<String> {
        return emptyList()
    }
    
    override suspend fun syncStocks(): Result<Unit> {
        return try {
            // Temporarily not loading stock data
            Timber.d("Stock data loading skipped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error loading dummy stocks")
            Result.failure(e)
        }
    }

    // Sale Purchase operations
    override fun getAllSalePurchases(): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getAllSalePurchases()
    }

    override suspend fun getSalePurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getSalePurchasesByDateRange(startDate, endDate)
    }

    override suspend fun getSalePurchasesByParty(partyName: String): Flow<List<SalePurchaseEntity>> {
        return database.salePurchaseDao().getSalePurchasesByParty(partyName)
    }

    override suspend fun getDailySales(date: Date, cmpCode: Int): BigDecimal? {
        return database.salePurchaseDao().getDailySales(date, cmpCode)
    }

    override suspend fun getDailyPurchases(date: Date, cmpCode: Int): BigDecimal? {
        return database.salePurchaseDao().getDailyPurchases(date, cmpCode)
    }

    override suspend fun syncSalePurchases(): Result<Unit> {
        return try {
            // Temporarily not loading sale/purchase data
            Timber.d("Sale/Purchase data loading skipped")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "Error loading dummy sale/purchase data")
            Result.failure(e)
        }
    }

    // Ledger operations
    override fun getLedgerEntriesByAccount(acId: Int): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByAccount(acId)
    }

    override suspend fun getLedgerEntriesByDateRange(acId: Int, startDate: Date, endDate: Date): Flow<List<LedgerEntity>> {
        return database.ledgerDao().getLedgerEntriesByAccountAndDateRange(acId, startDate, endDate)
    }

    override suspend fun getAccountBalance(acId: Int): BigDecimal? {
        return database.ledgerDao().getAccountBalance(acId)
    }

    override suspend fun syncLedgers(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getLedgers()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val ledgers = apiResult.getOrNull()
                if (ledgers != null) {
                    database.ledgerDao().insertLedgerEntries(ledgers)
                    Timber.d("Successfully loaded ${ledgers.size} ledger entries from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy ledgers: ${apiResult.exceptionOrNull()?.message}")
                database.ledgerDao().insertLedgerEntries(DummyDataProvider.getDummyLedgers())
                Timber.d("Successfully loaded dummy ledger entries as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing ledger entries")
            Result.failure(e)
        }
    }

    // Expiry operations
    override fun getAllExpiryItems(): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getAllExpiryItems()
    }

    override suspend fun getItemsExpiringWithinDays(days: Int): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getItemsExpiringWithinDays(days)
    }

    override suspend fun getExpiredItems(): Flow<List<ExpiryEntity>> {
        return database.expiryDao().getExpiredItems()
    }

    override suspend fun syncExpiries(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getExpiries()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val expiries = apiResult.getOrNull()
                if (expiries != null) {
                    database.expiryDao().insertExpiryItems(expiries)
                    Timber.d("Successfully loaded ${expiries.size} expiry items from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy expiries: ${apiResult.exceptionOrNull()?.message}")
                database.expiryDao().insertExpiryItems(DummyDataProvider.getDummyExpiries())
                Timber.d("Successfully loaded dummy expiry items as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing expiry items")
            Result.failure(e)
        }
    }

    // Template operations
    override fun getAllTemplates(): Flow<List<TemplateEntity>> {
        return database.templateDao().getAllTemplates()
    }

    override suspend fun getTemplatesByCategory(category: String): Flow<List<TemplateEntity>> {
        return database.templateDao().getTemplatesByCategory(category)
    }

    override suspend fun syncTemplates(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getTemplates()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val templates = apiResult.getOrNull()
                if (templates != null) {
                    database.templateDao().insertTemplates(templates)
                    Timber.d("Successfully loaded ${templates.size} templates from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy templates: ${apiResult.exceptionOrNull()?.message}")
                database.templateDao().insertTemplates(DummyDataProvider.getDummyTemplates())
                Timber.d("Successfully loaded dummy templates as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing templates")
            Result.failure(e)
        }
    }

    // Price Screen operations
    override suspend fun getPriceScreenData(): Result<List<Any>> {
        // Create dummy price data
        val dummyPriceData = listOf(
            mapOf(
                "itemName" to "Rogar 100ml",
                "mrp" to 220.00,
                "purchasePrice" to 180.00,
                "sellingPrice" to 200.00,
                "margin" to 20.00
            ),
            mapOf(
                "itemName" to "Roundup Herbicide",
                "mrp" to 500.00,
                "purchasePrice" to 420.00,
                "sellingPrice" to 450.00,
                "margin" to 30.00
            ),
            mapOf(
                "itemName" to "NPK Fertilizer",
                "mrp" to 95.00,
                "purchasePrice" to 75.00,
                "sellingPrice" to 85.75,
                "margin" to 10.75
            ),
            mapOf(
                "itemName" to "Growth Booster",
                "mrp" to 300.00,
                "purchasePrice" to 250.00,
                "sellingPrice" to 275.00,
                "margin" to 25.00
            ),
            mapOf(
                "itemName" to "Hybrid Tomato Seeds",
                "mrp" to 18.00,
                "purchasePrice" to 12.00,
                "sellingPrice" to 15.50,
                "margin" to 3.50
            )
        )
        
        return Result.success(dummyPriceData)
    }

    // Sync all data
    override suspend fun syncAllData(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getAllData()
            
            if (apiResult.isSuccess) {
                // API call was successful, process the data
                Timber.d("Successfully fetched all data from API")
                
                // Process each entity type from the API response
                val allData = apiResult.getOrNull()
                if (allData != null) {
                    // Process users
                    allData["users"]?.let { usersList ->
                        @Suppress("UNCHECKED_CAST")
                        val users = usersList as List<UserEntity>
                        database.userDao().insertUsers(users)
                        Timber.d("Inserted ${users.size} users from API")
                    }
                    
                    // Process accounts
                    allData["accounts"]?.let { accountsList ->
                        @Suppress("UNCHECKED_CAST")
                        val accounts = accountsList as List<AccountMasterEntity>
                        database.accountMasterDao().insertAccounts(accounts)
                        Timber.d("Inserted ${accounts.size} accounts from API")
                    }
                    
                    // Process closing balances
                    allData["closingBalances"]?.let { balancesList ->
                        @Suppress("UNCHECKED_CAST")
                        val balances = balancesList as List<ClosingBalanceEntity>
                        database.closingBalanceDao().insertClosingBalances(balances)
                        Timber.d("Inserted ${balances.size} closing balances from API")
                    }
                    
                    // Process ledgers
                    allData["ledgers"]?.let { ledgersList ->
                        @Suppress("UNCHECKED_CAST")
                        val ledgers = ledgersList as List<LedgerEntity>
                        database.ledgerDao().insertLedgerEntries(ledgers)
                        Timber.d("Inserted ${ledgers.size} ledger entries from API")
                    }
                    
                    // Process expiries
                    allData["expiries"]?.let { expiriesList ->
                        @Suppress("UNCHECKED_CAST")
                        val expiries = expiriesList as List<ExpiryEntity>
                        database.expiryDao().insertExpiryItems(expiries)
                        Timber.d("Inserted ${expiries.size} expiry items from API")
                    }
                    
                    // Process templates
                    allData["templates"]?.let { templatesList ->
                        @Suppress("UNCHECKED_CAST")
                        val templates = templatesList as List<TemplateEntity>
                        database.templateDao().insertTemplates(templates)
                        Timber.d("Inserted ${templates.size} templates from API")
                    }
                }
                
                Result.success(Unit)
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy data: ${apiResult.exceptionOrNull()?.message}")
                
                try {
                    // Use dummy data as fallback - catch exceptions for each entity type
                    try {
                        syncUsers()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing users with dummy data")
                    }
                    
                    try {
                        syncAccounts()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing accounts with dummy data")
                    }
                    
                    try {
                        syncClosingBalances()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing closing balances with dummy data")
                    }
                    
                    try {
                        syncLedgers()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing ledgers with dummy data")
                    }
                    
                    try {
                        syncExpiries()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing expiries with dummy data")
                    }
                    
                    try {
                        syncTemplates()
                    } catch (e: Exception) {
                        Timber.e(e, "Error syncing templates with dummy data")
                    }
                    
                    // Note: Stock and SalePurchase entities are not being synced in this version
                    
                    Timber.d("Successfully loaded all dummy data as fallback")
                    // Return success even though we used fallback data
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "Error loading fallback dummy data")
                    // Return the original API error, not the fallback error
                    Result.failure(apiResult.exceptionOrNull() ?: e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during data synchronization")
            Result.failure(e)
        }
    }
}
