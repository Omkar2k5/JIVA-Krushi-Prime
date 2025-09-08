package com.example.jiva.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import com.google.gson.annotations.SerializedName

@Serializable
data class DayEndInfoRequest(
    @SerialName("cmpCode") @SerializedName("cmpCode") val cmpCode: Int,
    @SerialName("dayDate") @SerializedName("dayDate") val dayDate: String
)

@Serializable
data class DayEndInfoData(
    @SerialName("cmpCode") @SerializedName("cmpCode") val cmpCode: String? = null,
    @SerialName("purchase") @SerializedName("purchase") val purchase: String? = null,
    @SerialName("sale") @SerializedName("sale") val sale: String? = null,
    @SerialName("cash_Opening") @SerializedName("cash_Opening") val cashOpening: String? = null,
    @SerialName("cash_Closing") @SerializedName("cash_Closing") val cashClosing: String? = null,
    @SerialName("bank_Opening") @SerializedName("bank_Opening") val bankOpening: String? = null,
    @SerialName("bank_Closing") @SerializedName("bank_Closing") val bankClosing: String? = null,
    @SerialName("expenses") @SerializedName("expenses") val expenses: String? = null,
    @SerialName("payments") @SerializedName("payments") val payments: String? = null,
    @SerialName("receipts") @SerializedName("receipts") val receipts: String? = null,
    @SerialName("dayDate") @SerializedName("dayDate") val dayDate: String? = null
)

@Serializable
data class DayEndInfoResponse(
    @SerialName("isSuccess") @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerialName("message") @SerializedName("message") val message: String? = null,
    @SerialName("data") @SerializedName("data") val data: DayEndInfoData? = null
)