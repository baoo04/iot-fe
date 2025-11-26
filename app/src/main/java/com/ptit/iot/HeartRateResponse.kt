package com.ptit.iot


import com.google.gson.annotations.SerializedName

data class HeartRateResponse(
    @SerializedName("data")
    val `data`: Data?
) {
    data class Data(
        @SerializedName("userId")
        val userId: String?,

        @SerializedName("bpm")
        val bpm: Double?,

        @SerializedName("spo2")
        val spo2: Double?,

        @SerializedName("warning")
        val warning: Int?,

        @SerializedName("acc_x")
        val accX: Double?,

        @SerializedName("acc_y")
        val accY: Double?,

        @SerializedName("acc_z")
        val accZ: Double?,

        @SerializedName("temp_mpu")
        val temp: Double?
    )
}