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
            under = this.area ?: "",
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
            qty = this.qty.toDouble(),
            unit = this.unit ?: "",
            rate = this.rate.toDouble(),
            amount = this.amount.toDouble(),
            discount = this.discount.toDouble()
        )
    }
    
    // Convert LedgerEntity to LedgerEntry for Ledger Report Screen
    fun LedgerEntity.toLedgerEntry(): LedgerEntry {
        return LedgerEntry(
            entryDate = this.entryDate?.let { dateFormat.format(it) } ?: "",
            entryType = this.entryType ?: "",
            entryNo = this.entryNo?.toString() ?: "",
            particular = this.narration ?: "",
            dr = this.dr.toDouble(),
            cr = this.cr.toDouble(),
            manualNo = this.manualNo ?: "",
            details = "Account: ${this.acId ?: ""}"
        )
    }
    
    // Convert ExpiryEntity to ExpiryEntry for Expiry Report Screen
    fun ExpiryEntity.toExpiryEntry(): ExpiryEntry {
        return ExpiryEntry(
            itemId = this.itemId.toString(),
            itemName = this.itemName,
            itemType = this.itemType ?: "",
            batchNumber = this.batchNo ?: "",
            expiryDate = this.expiryDate ?: "",
            qty = this.qty.toInt(),
            daysToExpiry = this.daysLeft?.toLong() ?: 0L
        )
    }
    
    // Convert AccountMasterEntity to CustomerContact for WhatsApp Marketing
    fun AccountMasterEntity.toCustomerContact(): CustomerContact {
        return CustomerContact(
            accountNumber = this.acId?.toString() ?: this.srno.toString(),
            accountName = this.accountName,
            mobileNumber = this.mobile ?: "",
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
