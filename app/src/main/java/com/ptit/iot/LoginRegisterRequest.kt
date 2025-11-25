package com.ptit.iot


import com.google.gson.annotations.SerializedName

data class LoginRegisterRequest(
    @SerializedName("email")
    val email: String?,
    @SerializedName("name")
    val name: String?,
    @SerializedName("password")
    val password: String?
)