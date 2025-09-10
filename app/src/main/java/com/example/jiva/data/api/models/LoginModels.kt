package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

// Request model for API login
// The API expects mobileNo and password

data class ApiLoginRequest(
    @SerializedName("mobileNo") val mobileNo: String,
    @SerializedName("password") val password: String
)

// Response model from API login
// Updated structure: { "isSuccess": true, "message": "Success", "data": { "userID": "1026", "companyName": "Demo Krushi Seva Kendra", "isActive": "1", "validTill": "9/1/2027 12:00:00 AM" } }

data class ApiLoginResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ApiUserData?
)

data class ApiUserData(
    @SerializedName("userID") val userID: String?,
    @SerializedName("companyName") val companyName: String?,
    @SerializedName("isActive") val isActive: String?,
    @SerializedName("validTill") val validTill: String?
)
