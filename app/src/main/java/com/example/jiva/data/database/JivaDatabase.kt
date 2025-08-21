package com.example.jiva.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.jiva.data.database.converters.Converters
import com.example.jiva.data.database.dao.*
import com.example.jiva.data.database.entities.*

/**
 * Room database class for JIVA application
 * Maps to MySQL database: jivabusiness
 */
@Database(
    entities = [
        UserEntity::class,
        AccountMasterEntity::class,
        ClosingBalanceEntity::class,
        ExpiryEntity::class,
        LedgerEntity::class,
        SalePurchaseEntity::class,
        TemplateEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JivaDatabase : RoomDatabase() {
    
    // DAO abstract methods
    abstract fun userDao(): UserDao
    abstract fun accountMasterDao(): AccountMasterDao
    abstract fun closingBalanceDao(): ClosingBalanceDao
    abstract fun expiryDao(): ExpiryDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun salePurchaseDao(): SalePurchaseDao
    abstract fun templateDao(): TemplateDao
    
    companion object {
        @Volatile
        private var INSTANCE: JivaDatabase? = null
        
        fun getDatabase(context: Context): JivaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JivaDatabase::class.java,
                    "jiva_database"
                )
                .fallbackToDestructiveMigration() // Remove in production
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
