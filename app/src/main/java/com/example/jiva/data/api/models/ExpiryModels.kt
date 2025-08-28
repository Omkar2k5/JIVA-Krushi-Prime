package com.example.jiva.data.api.models

import kotlinx.serialization.Serializable

/**
 * Request model for Expiry API
 */
@Serializable
data class ExpiryRequest(
    val userID: Int,
    val yearString: String
)

/**
 * Response model for Expiry API
 */
@Serializable
data class ExpiryResponse(
    val isSuccess: Boolean,
    val message: String,
    val data: List<ExpiryItem>
)

/**
 * Individual expiry item from API response
 */
@Serializable
data class ExpiryItem(
    val cmpCode: String,
    val item_ID: String,
    val item_Name: String,
    val item_Type: String,
    val batch_No: String,
    val expiry_Date: String,
    val qty: String,
    val daysLeft: String,
    val yearString: String
)
