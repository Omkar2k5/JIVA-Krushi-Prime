package com.example.jiva.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MsgTemplatesRequest(
    @SerialName("userID") val userId: Int
)

@Serializable
data class MsgTemplateItem(
    @SerialName("cmpCode") val cmpCode: String? = null,
    @SerialName("tempID") val tempID: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("msg") val msg: String? = null,
    @SerialName("instanceID") val instanceID: String? = null,
    @SerialName("accessToken") val accessToken: String? = null,
    @SerialName("closing_Stock") val closingStock: String? = null,
)

@Serializable
data class MsgTemplatesResponse(
    @SerialName("isSuccess") val isSuccess: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: List<MsgTemplateItem>? = null
)