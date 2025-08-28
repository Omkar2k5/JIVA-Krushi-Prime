package com.example.jiva.data.database.dao

import androidx.room.*
import com.example.jiva.data.database.entities.LedgerEntity
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.util.Date

/**
 * DAO for LedgerEntity operations
 */
@Dao
interface LedgerDao {
    
    @Query("SELECT * FROM tb_ledger ORDER BY EntryDate DESC")
    fun getAllLedgerEntries(): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE SerialNo = :serialNo")
    suspend fun getLedgerEntryBySerialNo(serialNo: Int): LedgerEntity?
    
    @Query("SELECT * FROM tb_ledger WHERE Ac_ID = :acId ORDER BY EntryDate DESC")
    fun getLedgerEntriesByAccount(acId: Int): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE CmpCode = :cmpCode ORDER BY EntryDate DESC")
    fun getLedgerEntriesByCompany(cmpCode: Int): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE EntryType = :entryType ORDER BY EntryDate DESC")
    fun getLedgerEntriesByType(entryType: String): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE EntryDate BETWEEN :startDate AND :endDate ORDER BY EntryDate DESC")
    fun getLedgerEntriesByDateRange(startDate: Date, endDate: Date): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE Ac_ID = :acId AND EntryDate BETWEEN :startDate AND :endDate ORDER BY EntryDate DESC")
    fun getLedgerEntriesByAccountAndDateRange(acId: Int, startDate: Date, endDate: Date): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE ManualNo = :manualNo")
    fun getLedgerEntriesByManualNo(manualNo: String): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE RefNo = :refNo")
    fun getLedgerEntriesByRefNo(refNo: String): Flow<List<LedgerEntity>>
    
    @Query("SELECT * FROM tb_ledger WHERE YearString = :yearString ORDER BY EntryDate DESC")
    fun getLedgerEntriesByYear(yearString: String): Flow<List<LedgerEntity>>
    
    @Query("SELECT SUM(DR) - SUM(CR) FROM tb_ledger WHERE Ac_ID = :acId")
    suspend fun getAccountBalance(acId: Int): BigDecimal?
    
    @Query("SELECT SUM(DR) FROM tb_ledger WHERE Ac_ID = :acId AND EntryDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalDebitByAccountAndDateRange(acId: Int, startDate: Date, endDate: Date): BigDecimal?
    
    @Query("SELECT SUM(CR) FROM tb_ledger WHERE Ac_ID = :acId AND EntryDate BETWEEN :startDate AND :endDate")
    suspend fun getTotalCreditByAccountAndDateRange(acId: Int, startDate: Date, endDate: Date): BigDecimal?
    
    @Query("SELECT DISTINCT EntryType FROM tb_ledger WHERE EntryType IS NOT NULL ORDER BY EntryType")
    suspend fun getAllEntryTypes(): List<String>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntry(ledgerEntry: LedgerEntity): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLedgerEntries(ledgerEntries: List<LedgerEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(ledgerEntries: List<LedgerEntity>)

    @Query("DELETE FROM tb_ledger WHERE YearString = :yearString")
    suspend fun deleteByYear(yearString: String)
    
    @Update
    suspend fun updateLedgerEntry(ledgerEntry: LedgerEntity)
    
    @Delete
    suspend fun deleteLedgerEntry(ledgerEntry: LedgerEntity)
    
    @Query("DELETE FROM tb_ledger WHERE SerialNo = :serialNo")
    suspend fun deleteLedgerEntryBySerialNo(serialNo: Int)
    
    @Query("DELETE FROM tb_ledger")
    suspend fun deleteAllLedgerEntries()
}
