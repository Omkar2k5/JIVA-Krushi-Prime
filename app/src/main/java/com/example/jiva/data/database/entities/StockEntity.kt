package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for tb_stock table
 * Maps to MySQL table: tb_stock
 */
@Entity(tableName = "tb_stock")
@Serializable
data class StockEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNO")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: String,

    @ColumnInfo(name = "ITEM_ID")
    val itemId: String,
    
    @ColumnInfo(name = "Item_Name")
    val itemName: String,
    
    @ColumnInfo(name = "Opening")
    val opening: String = "0.000",

    @ColumnInfo(name = "InWard")
    val inWard: String = "0.000",

    @ColumnInfo(name = "OutWard")
    val outWard: String = "0.000",

    @ColumnInfo(name = "Closing_Stock")
    val closingStock: String = "0.000",

    @ColumnInfo(name = "AvgRate")
    val avgRate: String = "0.00",

    @ColumnInfo(name = "Valuation")
    val valuation: String = "0.00",
    
    @ColumnInfo(name = "ItemType")
    val itemType: String = "",

    @ColumnInfo(name = "Company")
    val company: String = "",

    @ColumnInfo(name = "cgst")
    val cgst: String = "0.00",

    @ColumnInfo(name = "sgst")
    val sgst: String = "0.00",

    @ColumnInfo(name = "igst")
    val igst: String = "0.00",

    @ColumnInfo(name = "YearString")
    val yearString: String = ""
)
