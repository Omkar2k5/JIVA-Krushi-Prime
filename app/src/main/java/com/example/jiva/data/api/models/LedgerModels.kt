package com.example.jiva.data.api.models

import kotlinx.serialization.Serializable

/**
 * Request model for Ledger API
 */
@Serializable
data class LedgerRequest(
    val userID: Int,
    val yearString: String
)

/**
 * Response model for Ledger API
 */
@Serializable
data class LedgerResponse(
    val isSuccess: Boolean,
    val message: String,
    val data: List<LedgerItem>
)

/**
 * Individual ledger item from API response
 */
@Serializable
data class LedgerItem(
    val cmpCode: String,
    val entryNo: String,
    val manualNo: String,
    val srNO: String,
    val entryType: String,
    val entryDate: String,
    val refNo: String,
    val ac_ID: String,
    val dr: String,
    val cr: String,
    val narration: String,
    val isClere: String,
    val trascType: String,
    val gstRate: String,
    val amt: String,
    val igst: String,
    val yearString: String
)
