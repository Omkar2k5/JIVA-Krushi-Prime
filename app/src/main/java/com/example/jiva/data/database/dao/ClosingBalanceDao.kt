package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.ClosingBalanceEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for ClosingBalanceEntity operations
 */
@Dao
interface ClosingBalanceDao {
    
    @Query("SELECT * FROM tb_closing_balance")
    fun getAllClosingBalances(): Flow<List<ClosingBalanceEntity>>
    
    @Query("SELECT * FROM tb_closing_balance WHERE SrNO = :srNo")
    suspend fun getClosingBalanceBySrNo(srNo: Int): ClosingBalanceEntity?
    
    @Query("SELECT * FROM tb_closing_balance WHERE AC_ID = :acId")
    suspend fun getClosingBalanceByAcId(acId: Int): ClosingBalanceEntity?
    
    @Query("SELECT * FROM tb_closing_balance WHERE CmpCode = :cmpCode")
    fun getClosingBalancesByCompany(cmpCode: Int): Flow<List<ClosingBalanceEntity>>
    
    @Query("SELECT * FROM tb_closing_balance WHERE Account_Name LIKE '%' || :searchTerm || '%'")
    fun searchClosingBalancesByName(searchTerm: String): Flow<List<ClosingBalanceEntity>>
    
    @Query("SELECT * FROM tb_closing_balance WHERE YearString = :yearString")
    fun getClosingBalancesByYear(yearString: String): Flow<List<ClosingBalanceEntity>>
    
    @Query("SELECT * FROM tb_closing_balance WHERE Days > :days")
    fun getOverdueBeyondDays(days: Int): Flow<List<ClosingBalanceEntity>>
    
    @Query("SELECT SUM(CAST(Balance AS DECIMAL(15,2))) FROM tb_closing_balance WHERE CmpCode = :cmpCode")
    suspend fun getTotalOutstandingByCompany(cmpCode: Int): Double?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingBalance(closingBalance: ClosingBalanceEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClosingBalances(closingBalances: List<ClosingBalanceEntity>)
    
    @Update
    suspend fun updateClosingBalance(closingBalance: ClosingBalanceEntity)
    
    @Delete
    suspend fun deleteClosingBalance(closingBalance: ClosingBalanceEntity)
    
    @Query("DELETE FROM tb_closing_balance WHERE SrNO = :srNo")
    suspend fun deleteClosingBalanceBySrNo(srNo: Int)
    
    @Query("DELETE FROM tb_closing_balance")
    suspend fun deleteAllClosingBalances()
}
