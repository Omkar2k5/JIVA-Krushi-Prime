package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

/**
 * Room entity for tb_closing_balance table
 * Maps to MySQL table: tb_closing_balance
 */
@Entity(tableName = "tb_closing_balance")
@Serializable
data class ClosingBalanceEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "SrNO")
    val srNo: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int,
    
    @ColumnInfo(name = "AC_ID")
    val acId: Int,
    
    @ColumnInfo(name = "Account_Name")
    val accountName: String,
    
    @ColumnInfo(name = "Mobile")
    val mobile: String? = null,
    
    @ColumnInfo(name = "Under")
    val under: String? = null,
    
    @ColumnInfo(name = "Balance")
    val balance: String? = null,
    
    @ColumnInfo(name = "LastDate")
    val lastDate: String? = null,
    
    @ColumnInfo(name = "Days")
    val days: Int = 0,
    
    @ColumnInfo(name = "Credit_Limit_Amount")
    val creditLimitAmount: String? = null,
    
    @ColumnInfo(name = "Credit_Limit_Days")
    val creditLimitDays: String? = null,
    
    @ColumnInfo(name = "YearString")
    val yearString: String? = null
)
