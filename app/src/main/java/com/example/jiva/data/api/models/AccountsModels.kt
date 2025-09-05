package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

// Request with optional filters (e.g., { "aC_ID": "2" })

data class AccountsRequest(
    @SerializedName("userID") val userID: Int,
    @SerializedName("yearString") val yearString: String,
    @SerializedName("filters") val filters: Map<String, String>? = null
)

// Response wrapper

data class AccountsResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: List<AccountRecord>?
)

// Single account record from Accounts API

data class AccountRecord(
    @SerializedName("cmpCode") val cmpCode: String?,
    @SerializedName("ac_ID") val acId: String?,
    @SerializedName("account_Name") val accountName: String?,
    @SerializedName("under") val under: String?,
    @SerializedName("area") val area: String?,
    @SerializedName("opening_Balance") val openingBalance: String?,
    @SerializedName("crdr") val crdr: String?,
    @SerializedName("detailed_Address") val detailedAddress: String?,
    @SerializedName("phone") val phone: String?,
    @SerializedName("mobile") val mobile: String?,
    @SerializedName("sT_Reg_No") val stRegNo: String?,
    @SerializedName("customerType") val customerType: String?,
    @SerializedName("state") val state: String?,
    @SerializedName("yearString") val yearString: String?
)