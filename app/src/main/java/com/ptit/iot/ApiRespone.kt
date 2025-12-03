package com.ptit.iot

import com.google.gson.annotations.SerializedName

data class ApiResponse<T>(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("errorString") val errorString: String?
)