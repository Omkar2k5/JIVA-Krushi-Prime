package com.example.jiva.data.api.models

import com.google.gson.annotations.SerializedName

// Request model for API login
// The API expects mobileNo and password

data class ApiLoginRequest(
    @SerializedName("mobileNo") val mobileNo: String,
    @SerializedName("password") val password: String
)

// Response model from API login
// Example: { "isSuccess": true, "message": "success", "data": { "userID": "123" } }

data class ApiLoginResponse(
    @SerializedName("isSuccess") val isSuccess: Boolean,
    @SerializedName("message") val message: String?,
    @SerializedName("data") val data: ApiUserData?
)

data class ApiUserData(
    @SerializedName("userID") val userID: String?
)