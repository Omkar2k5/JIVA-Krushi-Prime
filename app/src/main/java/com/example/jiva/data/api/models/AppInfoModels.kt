package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

/**
 * Response model for GetAppInfo API
 * GET /api/JivaBusiness/GetAppInfo
 */
data class AppInfoResponse(
    @SerializedName("isSuccess")
    val isSuccess: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: AppInfoData?
)

data class AppInfoData(
    @SerializedName("appName")
    val appName: String,
    
    @SerializedName("appVersion")
    val appVersion: String,
    
    @SerializedName("ip")
    val ip: String
)
