package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

/**
 * Room entity for tb_stock table
 * Maps to MySQL table: tb_stock
 */
@Entity(tableName = "tb_stock")
data class StockEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNO")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "ITEM_ID")
    val itemId: Int,
    
    @ColumnInfo(name = "Item_Name")
    val itemName: String,
    
    @ColumnInfo(name = "Opening")
    val opening: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "InWard")
    val inWard: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "OutWard")
    val outWard: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Closing_Stock")
    val closingStock: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "AvgRate")
    val avgRate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Valuation")
    val valuation: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "ItemType")
    val itemType: String? = null,
    
    @ColumnInfo(name = "Company")
    val company: String? = null,
    
    @ColumnInfo(name = "cgst")
    val cgst: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "sgst")
    val sgst: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "igst")
    val igst: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
