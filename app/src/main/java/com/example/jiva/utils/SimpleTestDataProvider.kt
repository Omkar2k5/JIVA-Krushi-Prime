package com.example.jiva.utils

import android.content.Context
import com.example.jiva.data.database.JivaDatabase
import com.example.jiva.data.database.entities.OutstandingEntity
import com.example.jiva.data.database.entities.StockEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Simple Test Data Provider
 * Provides minimal test data without schema conflicts
 * Only used when no permanent storage data exists
 */
object SimpleTestDataProvider {
    
    /**
     * Create simple test Outstanding data
     */
    fun getSimpleOutstandingData(): List<OutstandingEntity> {
        return listOf(
            OutstandingEntity(
                cmpCode = "1017",
                acId = "1",
                accountName = "Test Customer 1",
                mobile = "9876543210",
                under = "Sundry Debtors",
                balance = "5000.00",
                lastDate = "20-Aug-2025",
                days = "10",
                creditLimitAmount = "25000.00",
                creditLimitDays = "30",
                yearString = "2025-26"
            ),
            OutstandingEntity(
                cmpCode = "1017",
                acId = "2",
                accountName = "Test Customer 2",
                mobile = "9876543211",
                under = "Sundry Debtors",
                balance = "3000.00",
                lastDate = "15-Aug-2025",
                days = "15",
                creditLimitAmount = "15000.00",
                creditLimitDays = "30",
                yearString = "2025-26"
            ),
            OutstandingEntity(
                cmpCode = "1017",
                acId = "3",
                accountName = "Test Customer 3",
                mobile = "9876543212",
                under = "Sundry Debtors",
                balance = "7500.00",
                lastDate = "10-Aug-2025",
                days = "20",
                creditLimitAmount = "50000.00",
                creditLimitDays = "45",
                yearString = "2025-26"
            )
        )
    }
    
    /**
     * Create simple test Stock data
     */
    fun getSimpleStockData(): List<StockEntity> {
        return listOf(
            StockEntity(
                cmpCode = "1017",
                itemId = "1",
                itemName = "Test Product 1",
                opening = "50.000",
                inWard = "20.000",
                outWard = "15.000",
                closingStock = "55.000",
                avgRate = "125.50",
                valuation = "6902.50",
                itemType = "Pesticides",
                company = "Test Company 1",
                cgst = "9.00",
                sgst = "9.00",
                igst = "0.00",
                yearString = "2025-26"
            ),
            StockEntity(
                cmpCode = "1017",
                itemId = "2",
                itemName = "Test Product 2",
                opening = "30.000",
                inWard = "10.000",
                outWard = "8.000",
                closingStock = "32.000",
                avgRate = "450.00",
                valuation = "14400.00",
                itemType = "Fertilizers",
                company = "Test Company 2",
                cgst = "18.00",
                sgst = "18.00",
                igst = "0.00",
                yearString = "2025-26"
            ),
            StockEntity(
                cmpCode = "1017",
                itemId = "3",
                itemName = "Test Product 3",
                opening = "100.000",
                inWard = "50.000",
                outWard = "40.000",
                closingStock = "110.000",
                avgRate = "85.75",
                valuation = "9432.50",
                itemType = "Seeds",
                company = "Test Company 3",
                cgst = "5.00",
                sgst = "5.00",
                igst = "0.00",
                yearString = "2025-26"
            )
        )
    }
    
    /**
     * Populate database with simple test data only if no data exists
     */
    suspend fun populateIfEmpty(
        context: Context,
        database: JivaDatabase,
        year: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // Check if any data already exists
            val outstandingCount = database.outstandingDao().getAllSync(year).size
            val stockCount = database.stockDao().getAllSync(year).size
            
            if (outstandingCount == 0 && stockCount == 0) {
                Timber.d("🔧 No data found - adding simple test data for development")
                
                // Add simple test data
                database.outstandingDao().insertAll(getSimpleOutstandingData())
                database.stockDao().insertAll(getSimpleStockData())
                
                // Also save to permanent storage for consistency
                PermanentStorageManager.saveOutstandingData(context, getSimpleOutstandingData(), year)
                PermanentStorageManager.saveStockData(context, getSimpleStockData(), year)
                
                Timber.d("✅ Simple test data added successfully")
                true
            } else {
                Timber.d("📊 Data already exists - skipping test data population")
                false
            }
        } catch (e: Exception) {
            Timber.e(e, "❌ Error adding simple test data")
            false
        }
    }
}
