package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Stock API Request Model
 */
data class StockRequest(
    @SerializedName("userID")
    val userID: Int,
    @SerializedName("yearString")
    val yearString: String
)

/**
 * Stock API Response Model
 */
data class StockResponse(
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    @SerializedName("message")
    val message: String,
    @SerializedName("data")
    val data: List<StockItem>?
)

/**
 * Individual Stock Item Model
 */
data class StockItem(
    @SerializedName("cmpCode")
    val cmpCode: String,
    @SerializedName("iteM_ID")
    val itemId: String,
    @SerializedName("item_Name")
    val itemName: String,
    @SerializedName("opening")
    val opening: String,
    @SerializedName("inWard")
    val inWard: String,
    @SerializedName("outWard")
    val outWard: String,
    @SerializedName("closing_Stock")
    val closingStock: String,
    @SerializedName("avgRate")
    val avgRate: String,
    @SerializedName("valuation")
    val valuation: String,
    @SerializedName("itemType")
    val itemType: String,
    @SerializedName("company")
    val company: String,
    @SerializedName("cgst")
    val cgst: String,
    @SerializedName("sgst")
    val sgst: String,
    @SerializedName("igst")
    val igst: String,
    @SerializedName("yearString")
    val yearString: String
)
