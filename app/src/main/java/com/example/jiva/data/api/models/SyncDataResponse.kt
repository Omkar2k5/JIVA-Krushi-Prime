package com.example.jiva.data.api.models

import com.example.jiva.data.database.entities.*
import kotlinx.serialization.Serializable

/**
 * Data structure for complete API sync response
 * Maps to the complete JSON response from /api/sync-all-data endpoint
 */
@Serializable
data class SyncDataResponse(
    val status: String,
    val message: String,
    val timestamp: String,
    val data: SyncData
)

@Serializable
data class SyncData(
    val users: List<UserEntity> = emptyList(),
    val accounts: List<AccountMasterEntity> = emptyList(),
    val closing_balances: List<ClosingBalanceEntity> = emptyList(),
    val stocks: List<StockEntity> = emptyList(),
    val sale_purchases: List<SalePurchaseEntity> = emptyList(),
    val ledgers: List<LedgerEntity> = emptyList(),
    val expiries: List<ExpiryEntity> = emptyList(),
    val templates: List<TemplateEntity> = emptyList(),
    val price_data: List<PriceDataEntity> = emptyList()
)