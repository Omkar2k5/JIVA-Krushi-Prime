package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Room entity for price list data
 * Maps to the API response structure for price list items
 */
@Entity(tableName = "tb_pricelist")
@Serializable
data class PriceDataEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Int = 0,

    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int = 0,

    @ColumnInfo(name = "ItemID")
    val itemId: String,

    @ColumnInfo(name = "ItemName")
    val itemName: String,

    @ColumnInfo(name = "MRP")
    @Contextual
    val mrp: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "CreditSaleRate")
    @Contextual
    val creditSaleRate: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "CashSaleRate")
    @Contextual
    val cashSaleRate: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "WholesaleRate")
    @Contextual
    val wholesaleRate: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "MaxPurchaseRate")
    @Contextual
    val maxPurchaseRate: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "AvgPurchaseRate")
    @Contextual
    val avgPurchaseRate: BigDecimal = BigDecimal.ZERO,

    @ColumnInfo(name = "YearString")
    val yearString: String = ""
)