package com.example.jiva.data.database.converters

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.util.Date

/**
 * Type converters for Room database
 * Handles conversion between Kotlin types and SQLite types
 */
class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromBigDecimal(value: BigDecimal?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toBigDecimal(value: String?): BigDecimal? {
        return value?.let { 
            try {
                BigDecimal(it)
            } catch (e: NumberFormatException) {
                BigDecimal.ZERO
            }
        }
    }
}
