package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Room entity for tb_expiry table
 * Maps to MySQL table: tb_expiry
 */
@Entity(tableName = "tb_expiry")
data class ExpiryEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNo")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "Item_ID")
    val itemId: Int,
    
    @ColumnInfo(name = "Item_Name")
    val itemName: String,
    
    @ColumnInfo(name = "Item_Type")
    val itemType: String? = null,
    
    @ColumnInfo(name = "Batch_No")
    val batchNo: String? = null,
    
    @ColumnInfo(name = "Expiry_Date")
    val expiryDate: String? = null,
    
    @ColumnInfo(name = "Qty")
    val qty: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "DaysLeft")
    val daysLeft: Int? = null,
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
