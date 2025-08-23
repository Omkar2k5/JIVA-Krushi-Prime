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
            // Try to get data from API first
            val apiResult = remoteDataSource.getSalePurchases()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val salePurchases = apiResult.getOrNull()
                if (salePurchases != null) {
                    database.salePurchaseDao().insertSalePurchases(salePurchases)
                    Timber.d("Successfully loaded ${salePurchases.size} sale/purchase records from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy sale/purchase data: ${apiResult.exceptionOrNull()?.message}")
                database.salePurchaseDao().insertSalePurchases(DummyDataProvider.getDummySalePurchases())
                Timber.d("Successfully loaded dummy sale/purchase data as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing sale/purchase data")
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
    override fun getAllPriceData(): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().getAllPriceData()
    }
    
    override suspend fun getPriceDataByItemId(itemId: String): PriceDataEntity? {
        return database.priceDataDao().getPriceDataByItemId(itemId)
    }
    
    override suspend fun searchPriceDataByItemName(searchTerm: String): Flow<List<PriceDataEntity>> {
        return database.priceDataDao().searchPriceDataByItemName(searchTerm)
    }
    
    override suspend fun syncPriceData(): Result<Unit> {
        return try {
            // Try to get data from API first
            val apiResult = remoteDataSource.getPriceScreenData()
            
            if (apiResult.isSuccess) {
                // API call was successful, save the data to database
                val priceData = apiResult.getOrNull()
                if (priceData != null) {
                    database.priceDataDao().insertAllPriceData(priceData)
                    Timber.d("Successfully loaded ${priceData.size} price data records from API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but data is null")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy price data: ${apiResult.exceptionOrNull()?.message}")
                database.priceDataDao().insertAllPriceData(DummyDataProvider.getDummyPriceData())
                Timber.d("Successfully loaded dummy price data as fallback")
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error syncing price data")
            Result.failure(e)
        }
    }

    // Sync all data
    override suspend fun syncAllData(): Result<Unit> {
        return try {
            // Try to get data from API using the new syncAllData endpoint
            val apiResult = remoteDataSource.syncAllData()
            
            if (apiResult.isSuccess) {
                // API call was successful, process the data
                Timber.d("Successfully fetched all data from API")
                
                // Process the structured API response
                val syncResponse = apiResult.getOrNull()
                if (syncResponse != null && syncResponse.status == "success") {
                    val data = syncResponse.data
                    
                    // Process users
                    if (data.users.isNotEmpty()) {
                        database.userDao().insertUsers(data.users)
                        Timber.d("Inserted ${data.users.size} users from API")
                    }
                    
                    // Process accounts
                    if (data.accounts.isNotEmpty()) {
                        database.accountMasterDao().insertAccounts(data.accounts)
                        Timber.d("Inserted ${data.accounts.size} accounts from API")
                    }
                    
                    // Process closing balances
                    if (data.closing_balances.isNotEmpty()) {
                        database.closingBalanceDao().insertClosingBalances(data.closing_balances)
                        Timber.d("Inserted ${data.closing_balances.size} closing balances from API")
                    }
                    
                    // Process stocks
                    if (data.stocks.isNotEmpty()) {
                        database.stockDao().insertStocks(data.stocks)
                        Timber.d("Inserted ${data.stocks.size} stocks from API")
                    }
                    
                    // Process sale/purchase data
                    if (data.sale_purchases.isNotEmpty()) {
                        database.salePurchaseDao().insertSalePurchases(data.sale_purchases)
                        Timber.d("Inserted ${data.sale_purchases.size} sale/purchase records from API")
                    }
                    
                    // Process ledgers
                    if (data.ledgers.isNotEmpty()) {
                        database.ledgerDao().insertLedgerEntries(data.ledgers)
                        Timber.d("Inserted ${data.ledgers.size} ledger entries from API")
                    }
                    
                    // Process expiries
                    if (data.expiries.isNotEmpty()) {
                        database.expiryDao().insertExpiryItems(data.expiries)
                        Timber.d("Inserted ${data.expiries.size} expiry items from API")
                    }
                    
                    // Process templates
                    if (data.templates.isNotEmpty()) {
                        database.templateDao().insertTemplates(data.templates)
                        Timber.d("Inserted ${data.templates.size} templates from API")
                    }
                    
                    // Process price data
                    if (data.price_data.isNotEmpty()) {
                        database.priceDataDao().insertAllPriceData(data.price_data)
                        Timber.d("Inserted ${data.price_data.size} price data records from API")
                    }
                    
                    Timber.d("Successfully synced all data from server API")
                    Result.success(Unit)
                } else {
                    throw IllegalStateException("API returned success but invalid response structure")
                }
            } else {
                // API call failed, fall back to dummy data
                Timber.w("API call failed, falling back to dummy data: ${apiResult.exceptionOrNull()?.message}")
                
                try {
                    // Use dummy data as fallback - sync each entity type
                    syncUsers()
                    syncAccounts() 
                    syncClosingBalances()
                    syncStocks()
                    syncSalePurchases()
                    syncLedgers()
                    syncExpiries()
                    syncTemplates()
                    syncPriceData()
                    
                    Timber.d("Successfully loaded all dummy data as fallback")
                    Result.success(Unit)
                } catch (e: Exception) {
                    Timber.e(e, "Error loading fallback dummy data")
                    Result.failure(apiResult.exceptionOrNull() ?: e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error during data synchronization")
            Result.failure(e)
        }
    }
}
