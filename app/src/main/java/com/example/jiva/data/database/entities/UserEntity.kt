package com.example.jiva.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import com.example.jiva.data.serializers.DateSerializer
import java.util.Date

/**
 * Room entity for users table
 * Maps to MySQL table: users
 */
@Entity(tableName = "users")
@Serializable
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "UserID")
    val userId: Int = 0,
    
    @ColumnInfo(name = "Password")
    val password: String,
    
    @ColumnInfo(name = "MobileNumber")
    val mobileNumber: String,
    
    @ColumnInfo(name = "CompanyName")
    val companyName: String,
    
    @ColumnInfo(name = "CompanyCode")
    val companyCode: String,
    
    @ColumnInfo(name = "OwnerName")
    val ownerName: String,
    
    @ColumnInfo(name = "DateOfRegistration")
    @Serializable(with = DateSerializer::class)
    @SerialName("DateOfRegistration")
    val dateOfRegistration: Date,
    
    @ColumnInfo(name = "IsActive")
    val isActive: Boolean = true
)
