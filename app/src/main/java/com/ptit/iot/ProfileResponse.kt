package com.ptit.iot

import com.google.gson.annotations.SerializedName

data class ProfileDto(
    @SerializedName("age") val age: Int?,
    @SerializedName("alco") val alco: Int?,
    @SerializedName("gender") val gender: Int?,
    @SerializedName("height") val height: Int?,
    @SerializedName("smoke") val smoke: Int?,
    @SerializedName("weight") val weight: Int?
)

data class ProfileResponse(
    @SerializedName("data") val `data`: Data?,
) {
    data class Data(
        @SerializedName("email") val email: String?,
        @SerializedName("name") val name: String?,
        @SerializedName("profile") val profile: ProfileDto?,
        @SerializedName("user_id") val userId: String?
    )
}

data class UpdateProfileRequest(
    @SerializedName("profile") val profile: ProfileDto,
    @SerializedName("name") val name: String? = null,
)