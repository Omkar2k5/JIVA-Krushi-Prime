package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

// Request model for Outstanding API with optional filters

data class OutstandingRequest(
    @SerializedName("userID") val userID: Int,
    @SerializedName("yearString") val yearString: String,
    @SerializedName("filters") val filters: Map<String, String>? = null
)

// Response wrapper

data class OutstandingResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<OutstandingData>?
)

// Single record returned by API

data class OutstandingData(
    @SerializedName("cmpCode") val cmpCode: String,
    @SerializedName("aC_ID") val acId: String, // Note: API uses aC_ID
    @SerializedName("account_Name") val accountName: String,
    @SerializedName("mobile") val mobile: String,
    @SerializedName("under") val under: String,
    @SerializedName("area") val area: String,
    @SerializedName("balance") val balance: String,
    @SerializedName("lastDate") val lastDate: String,
    @SerializedName("days") val days: String,
    @SerializedName("credit_Limit_Amount") val creditLimitAmount: String,
    @SerializedName("credit_Limit_Days") val creditLimitDays: String,
    @SerializedName("yearString") val yearString: String
)

// Account Names API models

data class AccountNamesRequest(
    @SerializedName("userID") val userID: Int,
    @SerializedName("yearString") val yearString: String
)

data class AccountNamesResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<AccountNameItem>?
)

data class AccountNameItem(
    // API may return either 'account_Name' or 'items' for the display name
    @SerializedName("account_Name") val accountName: String? = null,
    @SerializedName("items") val items: String? = null,
    // Also include AC id if provided
    @SerializedName("aC_ID") val acId: String? = null,
    // Optional fields if API returns them
    @SerializedName("area") val area: String? = null,
    @SerializedName("under") val under: String? = null
)