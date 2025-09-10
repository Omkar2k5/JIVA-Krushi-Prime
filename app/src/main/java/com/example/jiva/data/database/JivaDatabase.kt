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
        PriceDataEntity::class,
        OutstandingEntity::class
    ],
    version = 6,
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
    abstract fun outstandingDao(): OutstandingDao
    
    companion object {
        @Volatile
        private var INSTANCE: JivaDatabase? = null
        
        // Migration from version 1 to 2 - adds PriceData table
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
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

        // Migration from version 2 to 3 - adds Outstanding table
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `Outstanding` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cmpCode` TEXT NOT NULL,
                        `acId` TEXT NOT NULL,
                        `accountName` TEXT NOT NULL,
                        `mobile` TEXT NOT NULL,
                        `under` TEXT NOT NULL,
                        `balance` TEXT NOT NULL,
                        `lastDate` TEXT NOT NULL,
                        `days` TEXT NOT NULL,
                        `creditLimitAmount` TEXT NOT NULL,
                        `creditLimitDays` TEXT NOT NULL,
                        `yearString` TEXT NOT NULL
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_Outstanding_cmpCode_acId_yearString ON Outstanding(cmpCode, acId, yearString)")
            }
        }

        // Migration from version 3 to 4 - updates Stock table to use String fields
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Drop and recreate tb_stock table with String fields for better performance
                database.execSQL("DROP TABLE IF EXISTS `tb_stock`")
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `tb_stock` (
                        `SrNO` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `CmpCode` TEXT NOT NULL,
                        `ITEM_ID` TEXT NOT NULL,
                        `Item_Name` TEXT NOT NULL,
                        `Opening` TEXT NOT NULL DEFAULT '0.000',
                        `InWard` TEXT NOT NULL DEFAULT '0.000',
                        `OutWard` TEXT NOT NULL DEFAULT '0.000',
                        `Closing_Stock` TEXT NOT NULL DEFAULT '0.000',
                        `AvgRate` TEXT NOT NULL DEFAULT '0.00',
                        `Valuation` TEXT NOT NULL DEFAULT '0.00',
                        `ItemType` TEXT NOT NULL DEFAULT '',
                        `Company` TEXT NOT NULL DEFAULT '',
                        `cgst` TEXT NOT NULL DEFAULT '0.00',
                        `sgst` TEXT NOT NULL DEFAULT '0.00',
                        `igst` TEXT NOT NULL DEFAULT '0.00',
                        `YearString` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_tb_stock_CmpCode_ITEM_ID_YearString ON tb_stock(CmpCode, ITEM_ID, YearString)")
            }
        }
        
        // Migration from version 4 to 5 - add GST split columns to tb_salepurchase
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add columns with defaults for existing rows
                database.execSQL("ALTER TABLE `tb_salepurchase` ADD COLUMN `cgst` TEXT NOT NULL DEFAULT '0.00'")
                database.execSQL("ALTER TABLE `tb_salepurchase` ADD COLUMN `sgst` TEXT NOT NULL DEFAULT '0.00'")
                database.execSQL("ALTER TABLE `tb_salepurchase` ADD COLUMN `igst` TEXT NOT NULL DEFAULT '0.00'")
            }
        }
        
        // Migration from version 5 to 6 - add total column to tb_salepurchase
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add total column with default value for existing rows
                database.execSQL("ALTER TABLE `tb_salepurchase` ADD COLUMN `total` TEXT NOT NULL DEFAULT '0.00'")
            }
        }

        fun getDatabase(context: Context): JivaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    JivaDatabase::class.java,
                    "jiva_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
