package com.example.jiva.data.repository

import com.example.jiva.data.database.entities.*
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.Date

/**
 * Main repository interface for JIVA business data
 * Provides abstraction over local database and remote API
 */
interface JivaRepository {
    
    // User operations
    suspend fun getUsers(): List<UserEntity>
    suspend fun getUserById(userId: Int): UserEntity?
    suspend fun getUserByMobile(mobileNumber: String): UserEntity?
    suspend fun syncUsers(): Result<Unit>
    
    // Account Master operations
    fun getAllAccounts(): Flow<List<AccountMasterEntity>>
    suspend fun getAccountByAcId(acId: Int): AccountMasterEntity?
    suspend fun searchAccountsByName(searchTerm: String): Flow<List<AccountMasterEntity>>
    suspend fun getAllAreas(): List<String>
    suspend fun syncAccounts(): Result<Unit>
    
    // Closing Balance operations (Outstanding Report)
    fun getAllClosingBalances(): Flow<List<ClosingBalanceEntity>>
    suspend fun getClosingBalanceByAcId(acId: Int): ClosingBalanceEntity?
    suspend fun getTotalOutstanding(cmpCode: Int): Double?
    suspend fun syncClosingBalances(): Result<Unit>
    
    // Stock operations
    fun getAllStocks(): Flow<List<StockEntity>>
    suspend fun getStockByItemId(itemId: Int): StockEntity?
    suspend fun getStocksByItemType(itemType: String): Flow<List<StockEntity>>
    suspend fun getTotalStockValuation(cmpCode: Int): BigDecimal?
    suspend fun getAllItemTypes(): List<String>
    suspend fun getAllCompanies(): List<String>
    suspend fun syncStocks(): Result<Unit>
    
    // Sale Purchase operations
    fun getAllSalePurchases(): Flow<List<SalePurchaseEntity>>
    suspend fun getSalePurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<SalePurchaseEntity>>
    suspend fun getSalePurchasesByParty(partyName: String): Flow<List<SalePurchaseEntity>>
    suspend fun getDailySales(date: Date, cmpCode: Int): BigDecimal?
    suspend fun getDailyPurchases(date: Date, cmpCode: Int): BigDecimal?
    suspend fun syncSalePurchases(): Result<Unit>
    
    // Ledger operations
    fun getLedgerEntriesByAccount(acId: Int): Flow<List<LedgerEntity>>
    suspend fun getLedgerEntriesByDateRange(acId: Int, startDate: Date, endDate: Date): Flow<List<LedgerEntity>>
    suspend fun getAccountBalance(acId: Int): BigDecimal?
    suspend fun syncLedgers(): Result<Unit>
    
    // Expiry operations
    fun getAllExpiryItems(): Flow<List<ExpiryEntity>>
    suspend fun getItemsExpiringWithinDays(days: Int): Flow<List<ExpiryEntity>>
    suspend fun getExpiredItems(): Flow<List<ExpiryEntity>>
    suspend fun syncExpiries(): Result<Unit>
    
    // Template operations (WhatsApp)
    fun getAllTemplates(): Flow<List<TemplateEntity>>
    suspend fun getTemplatesByCategory(category: String): Flow<List<TemplateEntity>>
    suspend fun syncTemplates(): Result<Unit>
    
    // Price Screen operations
    fun getAllPriceData(): Flow<List<PriceDataEntity>>
    suspend fun getPriceDataByItemId(itemId: String): PriceDataEntity?
    suspend fun searchPriceDataByItemName(searchTerm: String): Flow<List<PriceDataEntity>>
    suspend fun syncPriceData(): Result<Unit>

    // Outstanding operations
    fun getOutstandingFlow(year: String): Flow<List<com.example.jiva.data.database.entities.OutstandingEntity>>
    suspend fun syncOutstanding(userId: Int, yearString: String): Result<Unit>

    // Stock operations
    fun getStockFlow(year: String): Flow<List<com.example.jiva.data.database.entities.StockEntity>>
    suspend fun syncStock(userId: Int, yearString: String): Result<Unit>

    // Ledger operations
    suspend fun syncLedger(userId: Int, yearString: String): Result<Unit>

    // Sale/Purchase operations
    suspend fun syncSalePurchase(userId: Int, yearString: String): Result<Unit>

    // Expiry operations
    suspend fun syncExpiry(userId: Int, yearString: String): Result<Unit>

    // Sync all data from server
    suspend fun syncAllData(): Result<Unit>
}
