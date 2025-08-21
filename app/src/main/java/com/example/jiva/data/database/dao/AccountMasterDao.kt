package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.AccountMasterEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for AccountMasterEntity operations
 */
@Dao
interface AccountMasterDao {
    
    @Query("SELECT * FROM tb_acmaster")
    fun getAllAccounts(): Flow<List<AccountMasterEntity>>
    
    @Query("SELECT * FROM tb_acmaster WHERE srno = :srno")
    suspend fun getAccountBySrno(srno: Int): AccountMasterEntity?
    
    @Query("SELECT * FROM tb_acmaster WHERE Ac_ID = :acId")
    suspend fun getAccountByAcId(acId: Int): AccountMasterEntity?
    
    @Query("SELECT * FROM tb_acmaster WHERE CmpCode = :cmpCode")
    fun getAccountsByCompany(cmpCode: Int): Flow<List<AccountMasterEntity>>
    
    @Query("SELECT * FROM tb_acmaster WHERE Area = :area")
    fun getAccountsByArea(area: String): Flow<List<AccountMasterEntity>>
    
    @Query("SELECT * FROM tb_acmaster WHERE Account_Name LIKE '%' || :searchTerm || '%'")
    fun searchAccountsByName(searchTerm: String): Flow<List<AccountMasterEntity>>
    
    @Query("SELECT * FROM tb_acmaster WHERE Mobile LIKE '%' || :mobile || '%'")
    fun searchAccountsByMobile(mobile: String): Flow<List<AccountMasterEntity>>
    
    @Query("SELECT DISTINCT Area FROM tb_acmaster WHERE Area IS NOT NULL ORDER BY Area")
    suspend fun getAllAreas(): List<String>
    
    @Query("SELECT DISTINCT CustomerType FROM tb_acmaster WHERE CustomerType IS NOT NULL ORDER BY CustomerType")
    suspend fun getAllCustomerTypes(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountMasterEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountMasterEntity>)
    
    @Update
    suspend fun updateAccount(account: AccountMasterEntity)
    
    @Delete
    suspend fun deleteAccount(account: AccountMasterEntity)
    
    @Query("DELETE FROM tb_acmaster WHERE srno = :srno")
    suspend fun deleteAccountBySrno(srno: Int)
    
    @Query("DELETE FROM tb_acmaster")
    suspend fun deleteAllAccounts()
}
