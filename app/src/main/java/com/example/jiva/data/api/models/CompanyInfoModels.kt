package com.example.jiva.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CompanyInfoRequest(
    @SerialName("userID") val userId: Int
)

@Serializable
data class CompanyInfoData(
    @SerialName("userID") val userId: String? = null,
    @SerialName("companyName") val companyName: String? = null,
    @SerialName("companyCode") val companyCode: String? = null,
    @SerialName("ownerName") val ownerName: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("password") val password: String? = null,
    @SerialName("regDate") val regDate: String? = null,
    @SerialName("expDate") val expDate: String? = null,
    @SerialName("isActive") val isActive: String? = null,
    @SerialName("address1") val address1: String? = null,
    @SerialName("address2") val address2: String? = null,
    @SerialName("address3") val address3: String? = null,
)

@Serializable
data class CompanyInfoResponse(
    @SerialName("isSuccess") val isSuccess: Boolean,
    @SerialName("message") val message: String? = null,
    @SerialName("data") val data: CompanyInfoData? = null
)