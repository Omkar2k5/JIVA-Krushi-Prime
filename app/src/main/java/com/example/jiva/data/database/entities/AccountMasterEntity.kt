package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.example.jiva.data.serializers.BigDecimalSerializer
import java.math.BigDecimal

/**
 * Room entity for tb_acmaster table
 * Maps to MySQL table: tb_acmaster
 */
@Entity(tableName = "tb_acmaster")
@Serializable
data class AccountMasterEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "srno")
    val srno: Int = 0,
    
    @ColumnInfo(name = "CmpCode")
    val cmpCode: Int? = null,
    
    @ColumnInfo(name = "Ac_ID")
    val acId: Int? = null,
    
    @ColumnInfo(name = "Account_Name")
    val accountName: String,
    
    @ColumnInfo(name = "Under")
    val under: String? = null,
    
    @ColumnInfo(name = "Area")
    val area: String? = null,
    
    @ColumnInfo(name = "Opening_Balance")
    @Contextual
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    
    @ColumnInfo(name = "CRDR")
    val crdr: String = "DR", // CR or DR
    
    @ColumnInfo(name = "Detailed_Address")
    val detailedAddress: String? = null,
    
    @ColumnInfo(name = "Phone")
    val phone: String? = null,
    
    @ColumnInfo(name = "Mobile")
    val mobile: String? = null,
    
    @ColumnInfo(name = "ST_Reg_No")
    val stRegNo: String? = null,
    
    @ColumnInfo(name = "CustomerType")
    val customerType: String? = null,
    
    @ColumnInfo(name = "State")
    val state: String? = null,
    
    @ColumnInfo(name = "yearString")
    val yearString: String? = null
)
