package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.ExpiryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ExpiryEntity operations
 */
@Dao
interface ExpiryDao {
    
    @Query("SELECT * FROM tb_expiry ORDER BY Expiry_Date ASC")
    fun getAllExpiryItems(): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE SrNo = :srNo")
    suspend fun getExpiryItemBySrNo(srNo: Int): ExpiryEntity?
    
    @Query("SELECT * FROM tb_expiry WHERE Item_ID = :itemId")
    fun getExpiryItemsByItemId(itemId: Int): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE CmpCode = :cmpCode ORDER BY Expiry_Date ASC")
    fun getExpiryItemsByCompany(cmpCode: Int): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE Item_Type = :itemType ORDER BY Expiry_Date ASC")
    fun getExpiryItemsByType(itemType: String): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE Item_Name LIKE '%' || :searchTerm || '%' ORDER BY Expiry_Date ASC")
    fun searchExpiryItemsByName(searchTerm: String): Flow<List<ExpiryEntity>>

    // Backwards-compatible aliases for ViewModel expectations
    @Query("SELECT * FROM tb_expiry WHERE Item_Name LIKE '%' || :itemName || '%' ORDER BY Expiry_Date ASC")
    fun getExpiryItemsByItemName(itemName: String): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE Batch_No LIKE '%' || :batchNo || '%' ORDER BY Expiry_Date ASC")
    fun searchExpiryItemsByBatch(batchNo: String): Flow<List<ExpiryEntity>>

    @Query("SELECT * FROM tb_expiry WHERE Batch_No LIKE '%' || :batchNo || '%' ORDER BY Expiry_Date ASC")
    fun getExpiryItemsByBatch(batchNo: String): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE DaysLeft <= :days ORDER BY DaysLeft ASC")
    fun getItemsExpiringWithinDays(days: Int): Flow<List<ExpiryEntity>>

    @Query("SELECT * FROM tb_expiry WHERE DaysLeft <= :days ORDER BY DaysLeft ASC")
    fun getItemsExpiringSoon(days: Int): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE DaysLeft < 0 ORDER BY DaysLeft ASC")
    fun getExpiredItems(): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE DaysLeft BETWEEN 0 AND 30 ORDER BY DaysLeft ASC")
    fun getItemsExpiringWithin30Days(): Flow<List<ExpiryEntity>>
    
    @Query("SELECT * FROM tb_expiry WHERE YearString = :yearString ORDER BY Expiry_Date ASC")
    fun getExpiryItemsByYear(yearString: String): Flow<List<ExpiryEntity>>
    
    @Query("SELECT DISTINCT Item_Type FROM tb_expiry WHERE Item_Type IS NOT NULL ORDER BY Item_Type")
    suspend fun getAllItemTypes(): List<String>

    @Query("SELECT DISTINCT Item_Name FROM tb_expiry WHERE Item_Name IS NOT NULL ORDER BY Item_Name")
    suspend fun getAllItemNames(): List<String>
    
    @Query("SELECT DISTINCT Batch_No FROM tb_expiry WHERE Batch_No IS NOT NULL ORDER BY Batch_No")
    suspend fun getAllBatchNumbers(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpiryItem(expiryItem: ExpiryEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpiryItems(expiryItems: List<ExpiryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(expiryItems: List<ExpiryEntity>)

    @Query("DELETE FROM tb_expiry WHERE YearString = :yearString")
    suspend fun deleteByYear(yearString: String)

    @Query("SELECT * FROM tb_expiry WHERE YearString = :yearString ORDER BY DaysLeft ASC")
    fun getByYear(yearString: String): Flow<List<ExpiryEntity>>

    @Update
    suspend fun updateExpiryItem(expiryItem: ExpiryEntity)

    @Delete
    suspend fun deleteExpiryItem(expiryItem: ExpiryEntity)

    @Query("DELETE FROM tb_expiry WHERE SrNo = :srNo")
    suspend fun deleteExpiryItemBySrNo(srNo: Int)

    @Query("DELETE FROM tb_expiry")
    suspend fun deleteAllExpiryItems()
}
