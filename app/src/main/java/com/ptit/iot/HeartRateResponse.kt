package com.ptit.iot


import com.google.gson.annotations.SerializedName

data class HeartRateResponse(
    @SerializedName("data")
    val `data`: Data?
) {
    data class Data(
        @SerializedName("bpm")
        val bpm: Double?,
        @SerializedName("spo2")
        val spo2: Int?,
        @SerializedName("userId")
        val userId: String?,
        @SerializedName("warning")
        val warning: Int?
    )
}