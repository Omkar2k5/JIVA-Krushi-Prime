package com.example.jiva.data.api.models

import kotlinx.serialization.Serializable

/**
 * Request model for Sale/Purchase API
 */
@Serializable
data class SalePurchaseRequest(
    val userID: Int,
    val yearString: String
)

/**
 * Response model for Sale/Purchase API
 */
@Serializable
data class SalePurchaseResponse(
    val isSuccess: Boolean,
    val message: String,
    val data: List<SalePurchaseItem>
)

/**
 * Individual sale/purchase item from API response
 */
@Serializable
data class SalePurchaseItem(
    val cmpCode: String,
    val trDate: String,
    val partyName: String,
    val gstin: String,
    val trType: String,
    val refNo: String,
    val item_Name: String,
    val hsn: String,
    val category: String,
    val qty: String,
    val unit: String,
    val rate: String,
    val amount: String,
    val discount: String,
    val yearString: String
)
