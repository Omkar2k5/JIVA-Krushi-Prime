package com.example.jiva.data.mapper

import com.example.jiva.data.database.entities.*
import com.example.jiva.screens.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data mapper to convert between Room entities and UI models
 * Bridges the gap between database entities and screen data models
 */
object DataMapper {
    
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    
    // Convert AccountMasterEntity to OutstandingEntry for Outstanding Report Screen
    fun AccountMasterEntity.toOutstandingEntry(closingBalance: ClosingBalanceEntity?): OutstandingEntry {
        return OutstandingEntry(
            acId = this.acId?.toString() ?: this.srno.toString(),
            accountName = this.accountName,
            mobile = this.mobile ?: "",
            under = this.under ?: "",
            area = this.area ?: "",
            balance = (closingBalance?.balance ?: this.openingBalance.toString()),
            lastDate = "",
            days = "",
            creditLimitAmount = "",
            creditLimitDays = ""
        )
    }
    
    // Convert StockEntity to StockEntry for Stock Report Screen (all strings)
    fun StockEntity.toStockEntry(): StockEntry {
        return StockEntry(
            itemId = this.itemId,
            itemName = this.itemName,
            opening = this.opening,
            inWard = this.inWard,
            outWard = this.outWard,
            closingStock = this.closingStock,
            avgRate = this.avgRate,
            valuation = this.valuation,
            itemType = this.itemType,
            company = this.company,
            cgst = this.cgst,
            sgst = this.sgst,
            igst = this.igst
        )
    }
    
    // Convert SalePurchaseEntity to SalesReportEntry for Sales Report Screen
    fun SalePurchaseEntity.toSalesReportEntry(): SalesReportEntry {
        return SalesReportEntry(
            trDate = dateFormat.format(this.trDate),
            partyName = this.partyName,
            gstin = this.gstin ?: "-",
            entryType = this.trType,
            refNo = this.refNo ?: "",
            itemName = this.itemName,
            hsnNo = this.hsn ?: "",
            itemType = this.category ?: "",
            qty = this.qty.toPlainString(),
            unit = this.unit ?: "",
            rate = this.rate.toPlainString(),
            amount = this.amount.toPlainString(),
            discount = this.discount.toPlainString(),
            cgst = this.cgst,
            sgst = this.sgst,
            igst = this.igst
        )
    }
    
    // Convert LedgerEntity to LedgerEntry for Ledger Report Screen
    fun LedgerEntity.toLedgerEntry(): LedgerEntry {
        return LedgerEntry(
            entryNo = this.entryNo?.toString() ?: "",
            manualNo = this.manualNo ?: "",
            srNo = this.srNo?.toString() ?: "",
            entryType = this.entryType ?: "",
            entryDate = this.entryDate?.let { dateFormat.format(it) } ?: "",
            refNo = this.refNo ?: "",
            acId = this.acId?.toString() ?: "",
            dr = this.dr?.toString() ?: "0.00",
            cr = this.cr?.toString() ?: "0.00",
            narration = this.narration ?: "",
            isClere = if (this.isClere == true) "True" else "False",
            trascType = this.trascType ?: "",
            gstRate = this.gstRate?.toString() ?: "0.00",
            amt = this.amt?.toString() ?: "0.00",
            igst = this.igst?.toString() ?: "0.00"
        )
    }
    
    // Convert ExpiryEntity to ExpiryEntry for Expiry Report Screen
    fun ExpiryEntity.toExpiryEntry(): ExpiryEntry {
        return ExpiryEntry(
            itemId = this.itemId?.toString() ?: "",
            itemName = this.itemName ?: "",
            itemType = this.itemType ?: "",
            batchNo = this.batchNo ?: "",
            expiryDate = this.expiryDate?.let { dateFormat.format(it) } ?: "",
            qty = this.qty?.toString() ?: "0",
            daysLeft = this.daysLeft?.toString() ?: "0"
        )
    }

    // Convert PriceDataEntity to PriceListEntry for PriceList Report Screen
    fun PriceDataEntity.toPriceListEntry(): PriceListEntry {
        return PriceListEntry(
            itemId = this.itemId,
            itemName = this.itemName,
            mrp = this.mrp.toString(),
            creditSaleRate = this.creditSaleRate.toString(),
            cashSaleRate = this.cashSaleRate.toString(),
            wholesaleRate = this.wholesaleRate.toString(),
            avgPurchaseRate = this.avgPurchaseRate.toString()
        )
    }

    // Convert SalePurchaseEntity to SalePurchaseEntry for SalePurchase Report Screen
    fun SalePurchaseEntity.toSalePurchaseEntry(): SalePurchaseEntry {
        return SalePurchaseEntry(
            trDate = this.trDate?.let { dateFormat.format(it) } ?: "",
            partyName = this.partyName ?: "",
            gstin = this.gstin ?: "",
            trType = this.trType ?: "",
            refNo = this.refNo ?: "",
            itemName = this.itemName ?: "",
            hsn = this.hsn ?: "",
            category = this.category ?: "",
            qty = this.qty?.toString() ?: "0",
            unit = this.unit ?: "",
            rate = this.rate?.toString() ?: "0.00",
            amount = this.amount?.toString() ?: "0.00",
            discount = this.discount?.toString() ?: "0.00"
        )
    }
    
    // Convert AccountMasterEntity to CustomerContact for WhatsApp Marketing
    fun AccountMasterEntity.toCustomerContact(): CustomerContact {
        // Sanitize mobile for UI: remove country code and non-digits; keep last 10 digits
        val raw = this.mobile ?: ""
        val digits = raw.filter { it.isDigit() }
        val displayMobile = if (digits.length >= 10) digits.takeLast(10) else digits
        return CustomerContact(
            accountNumber = this.acId?.toString() ?: this.srno.toString(),
            accountName = this.accountName,
            mobileNumber = displayMobile,
            isSelected = false
        )
    }
    
    // Helper function to calculate day-end data from sales/purchase entities
    fun calculateDayEndData(
        salePurchases: List<SalePurchaseEntity>,
        date: Date,
        cmpCode: Int
    ): DayEndData {
        val dateString = dateFormat.format(date)
        val dayTransactions = salePurchases.filter { 
            dateFormat.format(it.trDate) == dateString && it.cmpCode == cmpCode 
        }
        
        val totalSale = dayTransactions
            .filter { it.trType.contains("Sale", ignoreCase = true) }
            .sumOf { it.amount.toDouble() }
            
        val totalPurchase = dayTransactions
            .filter { it.trType.contains("Purchase", ignoreCase = true) }
            .sumOf { it.amount.toDouble() }
        
        return DayEndData(
            totalSale = totalSale,
            totalPurchase = totalPurchase,
            cashReceived = 0.0, // Will need additional calculation from ledger
            cashInHand = 0.0,   // Will need additional calculation from ledger
            date = dateString
        )
    }
}
