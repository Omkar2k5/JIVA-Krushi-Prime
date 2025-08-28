package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.SalePurchaseEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.Date

/**
 * DAO for SalePurchaseEntity operations
 */
@Dao
interface SalePurchaseDao {
    
    @Query("SELECT * FROM tb_salepurchase ORDER BY trDate DESC")
    fun getAllSalePurchases(): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE SrNo = :srNo")
    suspend fun getSalePurchaseBySrNo(srNo: Int): SalePurchaseEntity?
    
    @Query("SELECT * FROM tb_salepurchase WHERE CmpCode = :cmpCode ORDER BY trDate DESC")
    fun getSalePurchasesByCompany(cmpCode: Int): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE trType = :trType ORDER BY trDate DESC")
    fun getSalePurchasesByType(trType: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE PartyName LIKE '%' || :partyName || '%' ORDER BY trDate DESC")
    fun getSalePurchasesByParty(partyName: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE Item_Name LIKE '%' || :itemName || '%' ORDER BY trDate DESC")
    fun getSalePurchasesByItem(itemName: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE trDate BETWEEN :startDate AND :endDate ORDER BY trDate DESC")
    fun getSalePurchasesByDateRange(startDate: Date, endDate: Date): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE Category = :category ORDER BY trDate DESC")
    fun getSalePurchasesByCategory(category: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE HSN = :hsn ORDER BY trDate DESC")
    fun getSalePurchasesByHSN(hsn: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT * FROM tb_salepurchase WHERE YearString = :yearString ORDER BY trDate DESC")
    fun getSalePurchasesByYear(yearString: String): Flow<List<SalePurchaseEntity>>
    
    @Query("SELECT SUM(Amount) FROM tb_salepurchase WHERE trType LIKE '%Sale%' AND CmpCode = :cmpCode")
    suspend fun getTotalSales(cmpCode: Int): BigDecimal?
    
    @Query("SELECT SUM(Amount) FROM tb_salepurchase WHERE trType LIKE '%Purchase%' AND CmpCode = :cmpCode")
    suspend fun getTotalPurchases(cmpCode: Int): BigDecimal?
    
    @Query("SELECT SUM(Amount) FROM tb_salepurchase WHERE trDate = :date AND trType LIKE '%Sale%' AND CmpCode = :cmpCode")
    suspend fun getDailySales(date: Date, cmpCode: Int): BigDecimal?
    
    @Query("SELECT SUM(Amount) FROM tb_salepurchase WHERE trDate = :date AND trType LIKE '%Purchase%' AND CmpCode = :cmpCode")
    suspend fun getDailyPurchases(date: Date, cmpCode: Int): BigDecimal?
    
    @Query("SELECT DISTINCT PartyName FROM tb_salepurchase WHERE PartyName IS NOT NULL ORDER BY PartyName")
    suspend fun getAllPartyNames(): List<String>
    
    @Query("SELECT DISTINCT Item_Name FROM tb_salepurchase WHERE Item_Name IS NOT NULL ORDER BY Item_Name")
    suspend fun getAllItemNames(): List<String>
    
    @Query("SELECT DISTINCT Category FROM tb_salepurchase WHERE Category IS NOT NULL ORDER BY Category")
    suspend fun getAllCategories(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePurchase(salePurchase: SalePurchaseEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSalePurchases(salePurchases: List<SalePurchaseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(salePurchases: List<SalePurchaseEntity>)

    @Query("DELETE FROM tb_salepurchase WHERE YearString = :yearString")
    suspend fun deleteByYear(yearString: String)

    @Query("SELECT * FROM tb_salepurchase WHERE YearString = :yearString ORDER BY trDate DESC")
    fun getByYear(yearString: String): Flow<List<SalePurchaseEntity>>
    
    @Update
    suspend fun updateSalePurchase(salePurchase: SalePurchaseEntity)
    
    @Delete
    suspend fun deleteSalePurchase(salePurchase: SalePurchaseEntity)
    
    @Query("DELETE FROM tb_salepurchase WHERE SrNo = :srNo")
    suspend fun deleteSalePurchaseBySrNo(srNo: Int)
    
    @Query("DELETE FROM tb_salepurchase")
    suspend fun deleteAllSalePurchases()
}
