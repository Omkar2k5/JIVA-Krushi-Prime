package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal
import java.util.Date

/**
 * Room entity for tb_salepurchase table
 * Maps to MySQL table: tb_salepurchase
 */
@Entity(tableName = "tb_salepurchase")
data class SalePurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNo")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "trDate")
    val trDate: Date,
    
    @ColumnInfo(name = "PartyName")
    val partyName: String,
    
    @ColumnInfo(name = "gstin")
    val gstin: String? = null,
    
    @ColumnInfo(name = "trType")
    val trType: String,
    
    @ColumnInfo(name = "RefNo")
    val refNo: String? = null,
    
    @ColumnInfo(name = "Item_Name")
    val itemName: String,
    
    @ColumnInfo(name = "HSN")
    val hsn: String? = null,
    
    @ColumnInfo(name = "Category")
    val category: String? = null,
    
    @ColumnInfo(name = "Qty")
    val qty: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Unit")
    val unit: String? = null,
    
    @ColumnInfo(name = "Rate")
    val rate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Amount")
    val amount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Discount")
    val discount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
