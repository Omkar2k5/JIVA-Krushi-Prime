package com.example.jiva.data.database

import com.example.jiva.data.database.entities.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Provides dummy data for all database entities
 * Used to populate the database with sample data for testing
 */
object DummyDataProvider {
    
    fun getDummyUsers(): List<UserEntity> {
        return listOf(
            UserEntity(
                userId = 1,
                password = "testing",
                mobileNumber = "9876543210",
                companyName = "JIVA Agro Solutions",
                companyCode = "JIVA001",
                ownerName = "Demo User",
                dateOfRegistration = Date(),
                isActive = true
            ),
            UserEntity(
                userId = 2,
                password = "admin123",
                mobileNumber = "9876543211",
                companyName = "Admin Company",
                companyCode = "ADMIN001",
                ownerName = "Admin User",
                dateOfRegistration = Date(),
                isActive = true
            )
        )
    }
    
    fun getDummyAccounts(): List<AccountMasterEntity> {
        return listOf(
            AccountMasterEntity(
                srno = 1, cmpCode = 1, acId = 25, accountName = "Aman Shaikh",
                under = "Sundry Debtors", area = "North", openingBalance = BigDecimal("11000.00"),
                crdr = "DR", detailedAddress = "123 Main Street, Mumbai", phone = "022-12345678",
                mobile = "9876543210", customerType = "Regular", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 2, cmpCode = 1, acId = 26, accountName = "ABC Traders",
                under = "Sundry Debtors", area = "South", openingBalance = BigDecimal("5000.00"),
                crdr = "DR", detailedAddress = "456 Oak Avenue, Pune", phone = "020-87654321",
                mobile = "9876543211", customerType = "Wholesale", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 3, cmpCode = 1, acId = 27, accountName = "XYZ Suppliers",
                under = "Sundry Debtors", area = "East", openingBalance = BigDecimal("8000.00"),
                crdr = "DR", detailedAddress = "789 Pine Road, Nashik", phone = "0253-11223344",
                mobile = "9876543212", customerType = "Regular", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 4, cmpCode = 1, acId = 28, accountName = "PQR Industries",
                under = "Sundry Debtors", area = "West", openingBalance = BigDecimal("12000.00"),
                crdr = "DR", detailedAddress = "321 Elm Street, Aurangabad", phone = "0240-55667788",
                mobile = "9876543213", customerType = "Industrial", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 5, cmpCode = 1, acId = 29, accountName = "LMN Corporation",
                under = "Sundry Debtors", area = "North", openingBalance = BigDecimal("6500.00"),
                crdr = "DR", detailedAddress = "654 Maple Drive, Nagpur", phone = "0712-99887766",
                mobile = "9876543214", customerType = "Corporate", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 6, cmpCode = 1, acId = 30, accountName = "DEF Enterprises",
                under = "Sundry Debtors", area = "South", openingBalance = BigDecimal("9200.00"),
                crdr = "DR", detailedAddress = "987 Cedar Lane, Kolhapur", phone = "0231-44556677",
                mobile = "9876543215", customerType = "Regular", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 7, cmpCode = 1, acId = 31, accountName = "GHI Solutions",
                under = "Sundry Debtors", area = "East", openingBalance = BigDecimal("4800.00"),
                crdr = "DR", detailedAddress = "147 Birch Street, Solapur", phone = "0217-33445566",
                mobile = "9876543216", customerType = "Regular", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 8, cmpCode = 1, acId = 32, accountName = "Tech Agro",
                under = "Sundry Debtors", area = "West", openingBalance = BigDecimal("15000.00"),
                crdr = "DR", detailedAddress = "258 Willow Avenue, Satara", phone = "02162-22334455",
                mobile = "9876543217", customerType = "Technology", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 9, cmpCode = 1, acId = 33, accountName = "Modern Farms",
                under = "Sundry Debtors", area = "North", openingBalance = BigDecimal("7300.00"),
                crdr = "DR", detailedAddress = "369 Spruce Road, Ahmednagar", phone = "0241-66778899",
                mobile = "9876543218", customerType = "Agriculture", state = "Maharashtra", yearString = "2024-25"
            ),
            AccountMasterEntity(
                srno = 10, cmpCode = 1, acId = 34, accountName = "Green Valley",
                under = "Sundry Debtors", area = "South", openingBalance = BigDecimal("10500.00"),
                crdr = "DR", detailedAddress = "741 Poplar Street, Sangli", phone = "0233-88990011",
                mobile = "9876543219", customerType = "Organic", state = "Maharashtra", yearString = "2024-25"
            )
        )
    }
    
    fun getDummyClosingBalances(): List<ClosingBalanceEntity> {
        return listOf(
            ClosingBalanceEntity(
                srNo = 1, cmpCode = 1, acId = 25, accountName = "Aman Shaikh",
                mobile = "9876543210", under = "Sundry Debtors", balance = "12400.00",
                lastDate = "05-Aug-2025", days = 15, creditLimitAmount = "50000.00",
                creditLimitDays = "30", yearString = "2024-25"
            ),
            ClosingBalanceEntity(
                srNo = 2, cmpCode = 1, acId = 26, accountName = "ABC Traders",
                mobile = "9876543211", under = "Sundry Debtors", balance = "5500.00",
                lastDate = "10-Aug-2025", days = 10, creditLimitAmount = "25000.00",
                creditLimitDays = "30", yearString = "2024-25"
            ),
            ClosingBalanceEntity(
                srNo = 3, cmpCode = 1, acId = 27, accountName = "XYZ Suppliers",
                mobile = "9876543212", under = "Sundry Debtors", balance = "8750.00",
                lastDate = "12-Aug-2025", days = 8, creditLimitAmount = "40000.00",
                creditLimitDays = "45", yearString = "2024-25"
            ),
            ClosingBalanceEntity(
                srNo = 4, cmpCode = 1, acId = 28, accountName = "PQR Industries",
                mobile = "9876543213", under = "Sundry Debtors", balance = "13200.00",
                lastDate = "08-Aug-2025", days = 12, creditLimitAmount = "75000.00",
                creditLimitDays = "60", yearString = "2024-25"
            ),
            ClosingBalanceEntity(
                srNo = 5, cmpCode = 1, acId = 29, accountName = "LMN Corporation",
                mobile = "9876543214", under = "Sundry Debtors", balance = "6800.00",
                lastDate = "15-Aug-2025", days = 5, creditLimitAmount = "35000.00",
                creditLimitDays = "30", yearString = "2024-25"
            )
        )
    }

    fun getDummyStocks(): List<StockEntity> {
        return listOf(
            StockEntity(
                srNo = 1, cmpCode = "1", itemId = "1", itemName = "Rogar 100ml",
                opening = "50.000", inWard = "20.000", outWard = "15.000",
                closingStock = "55.000", avgRate = "125.50", valuation = "6902.50",
                itemType = "General", company = "Bayer Corp", cgst = "9.00", sgst = "9.00",
                igst = "0.00", yearString = "2024-25"
            ),
            StockEntity(
                srNo = 2, cmpCode = "1", itemId = "2", itemName = "Roundup Herbicide",
                opening = "30.000", inWard = "10.000", outWard = "8.000",
                closingStock = "32.000", avgRate = "450.00", valuation = "14400.00",
                itemType = "Pesticides", company = "Monsanto", cgst = "18.00", sgst = "18.00",
                igst = "0.00", yearString = "2024-25"
            ),
            StockEntity(
                srNo = 3, cmpCode = "1", itemId = "3", itemName = "NPK Fertilizer",
                opening = "100.000", inWard = "50.000", outWard = "40.000",
                closingStock = "110.000", avgRate = "85.75", valuation = "9432.50",
                itemType = "Fertilizers", company = "IFFCO", cgst = "5.00", sgst = "5.00",
                igst = "0.00", yearString = "2024-25"
            ),
            StockEntity(
                srNo = 4, cmpCode = "1", itemId = "4", itemName = "Growth Booster",
                opening = "25.000", inWard = "15.000", outWard = "10.000",
                closingStock = "30.000", avgRate = "275.00", valuation = "8250.00",
                itemType = "PGR", company = "UPL Limited", cgst = "12.00", sgst = "12.00",
                igst = "0.00", yearString = "2024-25"
            ),
            StockEntity(
                srNo = 5, cmpCode = "1", itemId = "5", itemName = "Hybrid Tomato Seeds",
                opening = "200.000", inWard = "100.000", outWard = "80.000",
                closingStock = "220.000", avgRate = "15.50", valuation = "3410.00",
                itemType = "Seeds", company = "Mahyco", cgst = "5.00", sgst = "5.00",
                igst = "0.00", yearString = "2024-25"
            )
        )
    }

    fun getDummySalePurchases(): List<SalePurchaseEntity> {
        val calendar = Calendar.getInstance()
        return listOf(
            SalePurchaseEntity(
                srNo = 1, cmpCode = 1, trDate = calendar.apply { set(2025, 7, 5) }.time,
                partyName = "Aman Shaikh", gstin = null, trType = "Credit Sale", refNo = "1",
                itemName = "Rogar 100ml", hsn = "3808", category = "Fertilizers",
                qty = BigDecimal("12.000"), unit = "Nos", rate = BigDecimal("200.00"),
                amount = BigDecimal("2285.71"), discount = BigDecimal("0.00"), yearString = "2024-25"
            ),
            SalePurchaseEntity(
                srNo = 2, cmpCode = 1, trDate = calendar.apply { set(2025, 7, 6) }.time,
                partyName = "ABC Traders", gstin = "27ABCDE1234F1Z5", trType = "Cash Sale", refNo = "2",
                itemName = "Roundup Herbicide", hsn = "3808", category = "Pesticides",
                qty = BigDecimal("5.000"), unit = "Ltr", rate = BigDecimal("450.00"),
                amount = BigDecimal("2250.00"), discount = BigDecimal("100.00"), yearString = "2024-25"
            ),
            SalePurchaseEntity(
                srNo = 3, cmpCode = 1, trDate = calendar.apply { set(2025, 7, 7) }.time,
                partyName = "XYZ Suppliers", gstin = "29XYZAB5678G2H6", trType = "Credit Sale", refNo = "3",
                itemName = "NPK Fertilizer", hsn = "3104", category = "Fertilizers",
                qty = BigDecimal("25.000"), unit = "Kg", rate = BigDecimal("85.75"),
                amount = BigDecimal("2143.75"), discount = BigDecimal("50.00"), yearString = "2024-25"
            ),
            SalePurchaseEntity(
                srNo = 4, cmpCode = 1, trDate = calendar.apply { set(2025, 7, 8) }.time,
                partyName = "PQR Industries", gstin = null, trType = "Cash Sale", refNo = "4",
                itemName = "Growth Booster", hsn = "3808", category = "PGR",
                qty = BigDecimal("8.000"), unit = "Nos", rate = BigDecimal("275.00"),
                amount = BigDecimal("2200.00"), discount = BigDecimal("0.00"), yearString = "2024-25"
            ),
            SalePurchaseEntity(
                srNo = 5, cmpCode = 1, trDate = calendar.apply { set(2025, 7, 9) }.time,
                partyName = "LMN Corporation", gstin = "27LMNOP9876Q1R7", trType = "Credit Purchase", refNo = "5",
                itemName = "Hybrid Tomato Seeds", hsn = "1209", category = "Seeds",
                qty = BigDecimal("50.000"), unit = "Pkt", rate = BigDecimal("15.50"),
                amount = BigDecimal("775.00"), discount = BigDecimal("0.00"), yearString = "2024-25"
            )
        )
    }
    
    fun getDummyLedgers(): List<LedgerEntity> {
        val calendar = Calendar.getInstance()
        return listOf(
            LedgerEntity(
                serialNo = 1, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 5) }.time,
                acId = 25, dr = BigDecimal("2400.00"),
                cr = BigDecimal("0.00"), entryType = "Sale", narration = "Credit Sale - Inv #1",
                yearString = "2024-25"
            ),
            LedgerEntity(
                serialNo = 2, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 10) }.time,
                acId = 25, dr = BigDecimal("0.00"),
                cr = BigDecimal("1000.00"), entryType = "Receipt", narration = "Cash Receipt",
                yearString = "2024-25"
            ),
            LedgerEntity(
                serialNo = 3, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 7) }.time,
                acId = 27, dr = BigDecimal("2250.00"),
                cr = BigDecimal("0.00"), entryType = "Sale", narration = "Credit Sale - Inv #3",
                yearString = "2024-25"
            ),
            LedgerEntity(
                serialNo = 4, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 12) }.time,
                acId = 27, dr = BigDecimal("0.00"),
                cr = BigDecimal("1500.00"), entryType = "Receipt", narration = "Bank Receipt - NEFT",
                yearString = "2024-25"
            ),
            LedgerEntity(
                serialNo = 5, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 9) }.time,
                acId = 29, dr = BigDecimal("0.00"),
                cr = BigDecimal("775.00"), entryType = "Purchase", narration = "Credit Purchase - Bill #5",
                yearString = "2024-25"
            ),
            LedgerEntity(
                serialNo = 6, cmpCode = 1, entryDate = calendar.apply { set(2025, 7, 15) }.time,
                acId = 29, dr = BigDecimal("500.00"),
                cr = BigDecimal("0.00"), entryType = "Payment", narration = "Cash Payment",
                yearString = "2024-25"
            )
        )
    }
    
    fun getDummyExpiries(): List<ExpiryEntity> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return listOf(
            ExpiryEntity(
                srNo = 1, cmpCode = 1, itemId = 1, itemName = "Rogar 100ml",
                batchNo = "B2025001", expiryDate = "2025-11-15",
                qty = BigDecimal("25.000"), daysLeft = 90, yearString = "2024-25"
            ),
            ExpiryEntity(
                srNo = 2, cmpCode = 1, itemId = 2, itemName = "Roundup Herbicide",
                batchNo = "RH2025002", expiryDate = "2025-12-30",
                qty = BigDecimal("15.000"), daysLeft = 135, yearString = "2024-25"
            ),
            ExpiryEntity(
                srNo = 3, cmpCode = 1, itemId = 3, itemName = "NPK Fertilizer",
                batchNo = "NPK2025003", expiryDate = "2026-02-15",
                qty = BigDecimal("50.000"), daysLeft = 180, yearString = "2024-25"
            ),
            ExpiryEntity(
                srNo = 4, cmpCode = 1, itemId = 4, itemName = "Growth Booster",
                batchNo = "GB2025004", expiryDate = "2025-10-20",
                qty = BigDecimal("10.000"), daysLeft = 45, yearString = "2024-25"
            ),
            ExpiryEntity(
                srNo = 5, cmpCode = 1, itemId = 5, itemName = "Hybrid Tomato Seeds",
                batchNo = "HTS2025005", expiryDate = "2025-09-25",
                qty = BigDecimal("100.000"), daysLeft = 20, yearString = "2024-25"
            )
        )
    }
    
    fun getDummyTemplates(): List<TemplateEntity> {
        return listOf(
            TemplateEntity(
                srNo = 1, cmpCode = 1, tempId = "T001", category = "Greetings",
                msg = "Dear Customer, wishing you a Happy Diwali! May this festival bring prosperity to your business. From JIVA Agro Solutions.",
                instanceId = "123456789", accessToken = "abcdef123456"
            ),
            TemplateEntity(
                srNo = 2, cmpCode = 1, tempId = "T002", category = "Promotions",
                msg = "Special Offer: Get 10% discount on all pesticides till 31st August. Visit our store today! From JIVA Agro Solutions.",
                instanceId = "123456789", accessToken = "abcdef123456"
            ),
            TemplateEntity(
                srNo = 3, cmpCode = 1, tempId = "T003", category = "Reminders",
                msg = "Dear Customer, your payment of Rs. {{amount}} is due. Please make the payment at your earliest convenience. From JIVA Agro Solutions.",
                instanceId = "123456789", accessToken = "abcdef123456"
            ),
            TemplateEntity(
                srNo = 4, cmpCode = 1, tempId = "T004", category = "Information",
                msg = "New stock of hybrid seeds has arrived! Visit our store to check out the latest varieties. From JIVA Agro Solutions.",
                instanceId = "123456789", accessToken = "abcdef123456"
            ),
            TemplateEntity(
                srNo = 5, cmpCode = 1, tempId = "T005", category = "Advisory",
                msg = "Weather Alert: Heavy rainfall expected in the next 48 hours. Please take necessary precautions for your crops. From JIVA Agro Solutions.",
                instanceId = "123456789", accessToken = "abcdef123456"
            )
        )
    }
    
    fun getDummyPriceData(): List<PriceDataEntity> {
        return listOf(
            PriceDataEntity(
                itemId = "ITM001", itemName = "Rogar 100ml",
                mrp = BigDecimal("220.00"), creditSaleRate = BigDecimal("200.00"),
                cashSaleRate = BigDecimal("195.00"), wholesaleRate = BigDecimal("180.00"),
                maxPurchaseRate = BigDecimal("150.00")
            ),
            PriceDataEntity(
                itemId = "ITM002", itemName = "NPK Fertilizer",
                mrp = BigDecimal("120.00"), creditSaleRate = BigDecimal("95.00"),
                cashSaleRate = BigDecimal("90.00"), wholesaleRate = BigDecimal("85.00"),
                maxPurchaseRate = BigDecimal("75.00")
            ),
            PriceDataEntity(
                itemId = "ITM003", itemName = "Hybrid Tomato Seeds",
                mrp = BigDecimal("18.00"), creditSaleRate = BigDecimal("15.50"),
                cashSaleRate = BigDecimal("15.00"), wholesaleRate = BigDecimal("14.00"),
                maxPurchaseRate = BigDecimal("12.00")
            ),
            PriceDataEntity(
                itemId = "ITM004", itemName = "Growth Booster",
                mrp = BigDecimal("300.00"), creditSaleRate = BigDecimal("275.00"),
                cashSaleRate = BigDecimal("270.00"), wholesaleRate = BigDecimal("250.00"),
                maxPurchaseRate = BigDecimal("220.00")
            ),
            PriceDataEntity(
                itemId = "ITM005", itemName = "Roundup Herbicide",
                mrp = BigDecimal("500.00"), creditSaleRate = BigDecimal("450.00"),
                cashSaleRate = BigDecimal("440.00"), wholesaleRate = BigDecimal("420.00"),
                maxPurchaseRate = BigDecimal("380.00")
            )
        )
    }
    
    /**
     * Populates the database with dummy data for all entities
     * @param database The Room database instance
     */
    suspend fun populateDatabase(database: JivaDatabase) {
        // Clear existing data
        database.clearAllTables()
        
        // Insert dummy data for all entities
        database.userDao().insertUsers(getDummyUsers())
        database.accountMasterDao().insertAccounts(getDummyAccounts())
        database.closingBalanceDao().insertClosingBalances(getDummyClosingBalances())
        database.stockDao().insertStocks(getDummyStocks())
        database.salePurchaseDao().insertSalePurchases(getDummySalePurchases())
        database.ledgerDao().insertLedgerEntries(getDummyLedgers())
        database.expiryDao().insertExpiryItems(getDummyExpiries())
        database.templateDao().insertTemplates(getDummyTemplates())
        database.priceDataDao().insertAllPriceData(getDummyPriceData())
    }
}