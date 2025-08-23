package com.example.jiva.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
        StockEntity::class,
        SalePurchaseEntity::class,
        LedgerEntity::class,
        ExpiryEntity::class,
        TemplateEntity::class,
        PriceDataEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class JivaDatabase : RoomDatabase() {
    
    // DAO abstract methods
    abstract fun userDao(): UserDao
    abstract fun accountMasterDao(): AccountMasterDao
    abstract fun closingBalanceDao(): ClosingBalanceDao
    abstract fun stockDao(): StockDao
    abstract fun salePurchaseDao(): SalePurchaseDao
    abstract fun ledgerDao(): LedgerDao
    abstract fun expiryDao(): ExpiryDao
    abstract fun templateDao(): TemplateDao
    abstract fun priceDataDao(): PriceDataDao
    
    companion object {
        @Volatile
        private var INSTANCE: JivaDatabase? = null
        
        // Migration from version 1 to 2 - adds PriceData table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create PriceData table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `PriceData` (
                        `itemId` TEXT PRIMARY KEY NOT NULL,
                        `itemName` TEXT NOT NULL,
                        `mrp` TEXT NOT NULL,
                        `creditSaleRate` TEXT NOT NULL,
                        `cashSaleRate` TEXT NOT NULL,
                        `wholesaleRate` TEXT NOT NULL,
                        `maxPurchaseRate` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
        
        fun getDatabase(context: Context): JivaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JivaDatabase::class.java,
                    "jiva_database"
                )
                .addMigrations(MIGRATION_1_2) // Add migration instead of destructive fallback
                .fallbackToDestructiveMigration() // Keep as fallback for development
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
