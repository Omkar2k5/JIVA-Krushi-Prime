package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.util.Date

/**
 * Room entity for tb_salepurchase table
 * Maps to MySQL table: tb_salepurchase
 */
@Entity(tableName = "tb_salepurchase")
@Serializable
data class SalePurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNo")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "trDate")
    @Contextual
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
    @Contextual
    val qty: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Unit")
    val unit: String? = null,
    
    @ColumnInfo(name = "Rate")
    @Contextual
    val rate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Amount")
    @Contextual
    val amount: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "Discount")
    @Contextual
    val discount: BigDecimal = BigDecimal.ZERO,

    // New tax split columns (stored as text for simplicity and consistency across responses)
    @ColumnInfo(name = "cgst")
    val cgst: String = "0.00",

    @ColumnInfo(name = "sgst")
    val sgst: String = "0.00",

    @ColumnInfo(name = "igst")
    val igst: String = "0.00",
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
