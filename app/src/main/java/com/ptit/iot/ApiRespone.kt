package com.ptit.iot

import com.google.gson.annotations.SerializedName

// Class này dùng để hứng cái vỏ bên ngoài của JSON từ server (statusCode, data, errorString)
data class ApiResponse<T>(
    @SerializedName("statusCode") val statusCode: Int,
    @SerializedName("data") val data: T?,
    @SerializedName("errorString") val errorString: String?
)