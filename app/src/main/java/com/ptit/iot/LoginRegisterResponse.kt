package com.ptit.iot


import com.google.gson.annotations.SerializedName

data class LoginRegisterResponse(
    @SerializedName("data")
    val `data`: Data?
) {
    data class Data(
        @SerializedName("access_token")
        val accessToken: String?,
        @SerializedName("refresh_token")
        val refreshToken: String?,
        @SerializedName("user_id")
        val userId: String?
    )
}