package com.example.jiva.data.database.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "Outstanding",
    indices = [Index(value = ["cmpCode", "acId", "yearString"], unique = false)]
)
data class OutstandingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val cmpCode: String,
    val acId: String,
    val accountName: String,
    val mobile: String,
    val under: String,
    val balance: String,
    val lastDate: String,
    val days: String,
    val creditLimitAmount: String,
    val creditLimitDays: String,
    val yearString: String
)