package com.example.jiva.data.api.models

import kotlinx.serialization.Serializable

/**
 * Request model for PriceList API
 */
@Serializable
data class PriceListRequest(
    val userID: Int,
    val yearString: String
)

/**
 * Response model for PriceList API
 */
@Serializable
data class PriceListResponse(
    val isSuccess: Boolean,
    val message: String,
    val data: List<PriceListItem>
)

/**
 * Individual price list item from API response
 */
@Serializable
data class PriceListItem(
    val cmpCode: String,
    val itemID: String,
    val itemName: String,
    val mrp: String,
    val credit_Sale_Rate: String,
    val cash_Sale_Rate: String,
    val wholeSale_Rate: String,
    val avg_Purchase_Rate: String,
    val yearString: String
)
