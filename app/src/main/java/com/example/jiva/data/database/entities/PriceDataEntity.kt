package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal

/**
 * Room entity for price screen data
 * Used for price management functionality
 */
@Entity(tableName = "price_data")
@Serializable
data class PriceDataEntity(
    @PrimaryKey
    @ColumnInfo(name = "itemId")
    val itemId: String,
    
    @ColumnInfo(name = "itemName")
    val itemName: String,
    
    @ColumnInfo(name = "mrp")
    @Contextual
    val mrp: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "creditSaleRate")
    @Contextual
    val creditSaleRate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "cashSaleRate")
    @Contextual
    val cashSaleRate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "wholesaleRate")
    @Contextual
    val wholesaleRate: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "maxPurchaseRate")
    @Contextual
    val maxPurchaseRate: BigDecimal = BigDecimal.ZERO
)