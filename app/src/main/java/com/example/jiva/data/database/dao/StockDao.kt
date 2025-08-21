package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.StockEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal

/**
 * DAO for StockEntity operations
 */
@Dao
interface StockDao {
    
    @Query("SELECT * FROM tb_stock")
    fun getAllStocks(): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE SrNO = :srNo")
    suspend fun getStockBySrNo(srNo: Int): StockEntity?
    
    @Query("SELECT * FROM tb_stock WHERE ITEM_ID = :itemId")
    suspend fun getStockByItemId(itemId: Int): StockEntity?
    
    @Query("SELECT * FROM tb_stock WHERE CmpCode = :cmpCode")
    fun getStocksByCompany(cmpCode: Int): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE ItemType = :itemType")
    fun getStocksByItemType(itemType: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE Company = :company")
    fun getStocksByCompany(company: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE Item_Name LIKE '%' || :searchTerm || '%'")
    fun searchStocksByItemName(searchTerm: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE YearString = :yearString")
    fun getStocksByYear(yearString: String): Flow<List<StockEntity>>
    
    @Query("SELECT * FROM tb_stock WHERE Closing_Stock <= :threshold")
    fun getLowStockItems(threshold: BigDecimal): Flow<List<StockEntity>>
    
    @Query("SELECT SUM(Valuation) FROM tb_stock WHERE CmpCode = :cmpCode")
    suspend fun getTotalStockValuation(cmpCode: Int): BigDecimal?
    
    @Query("SELECT DISTINCT ItemType FROM tb_stock WHERE ItemType IS NOT NULL ORDER BY ItemType")
    suspend fun getAllItemTypes(): List<String>
    
    @Query("SELECT DISTINCT Company FROM tb_stock WHERE Company IS NOT NULL ORDER BY Company")
    suspend fun getAllCompanies(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStock(stock: StockEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStocks(stocks: List<StockEntity>)
    
    @Update
    suspend fun updateStock(stock: StockEntity)
    
    @Delete
    suspend fun deleteStock(stock: StockEntity)
    
    @Query("DELETE FROM tb_stock WHERE SrNO = :srNo")
    suspend fun deleteStockBySrNo(srNo: Int)
    
    @Query("DELETE FROM tb_stock")
    suspend fun deleteAllStocks()
}
