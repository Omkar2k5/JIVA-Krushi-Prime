package com.example.jiva.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class YearRequest(
    @SerialName("userID") val userId: Int
)

@Serializable
data class YearData(
    @SerialName("yearString") val yearString: String
)

@Serializable
data class YearResponse(
    @SerialName("isSuccess") val isSuccess: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: List<YearData>? = null
)