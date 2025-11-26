package com.ptit.iot

import com.google.gson.annotations.SerializedName

data class TrainResponse(
    @SerializedName("message") val message: String?,
    @SerializedName("accuracy") val accuracy: Double?,
    @SerializedName("samples") val samples: Int?
)