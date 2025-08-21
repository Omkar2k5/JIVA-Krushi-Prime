package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

/**
 * Room entity for tb_ledger table
 * Maps to MySQL table: tb_ledger
 */
@Entity(tableName = "tb_ledger")
data class LedgerEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SerialNo")
    val serialNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int? = null,
    
    @ColumnInfo(name = "EntryNo")
    val entryNo: Int? = null,
    
    @ColumnInfo(name = "ManualNo")
    val manualNo: String? = null,
    
    @ColumnInfo(name = "SrNO")
    val srNo: Int? = null,
    
    @ColumnInfo(name = "EntryType")
    val entryType: String? = null,
    
    @ColumnInfo(name = "EntryDate")
    val entryDate: Date? = null,
    
    @ColumnInfo(name = "RefNo")
    val refNo: String? = null,
    
    @ColumnInfo(name = "Ac_ID")
    val acId: Int? = null,
    
    @ColumnInfo(name = "DR")
    val dr: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "CR")
    val cr: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Narration")
    val narration: String? = null,
    
    @ColumnInfo(name = "IsClere")
    val isClere: Boolean = false,
    
    @ColumnInfo(name = "TrascType")
    val trascType: String? = null,
    
    @ColumnInfo(name = "GSTRate")
    val gstRate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Amt")
    val amt: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "IGST")
    val igst: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
