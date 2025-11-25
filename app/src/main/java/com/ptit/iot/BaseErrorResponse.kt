package com.ptit.iot

import com.google.gson.annotations.SerializedName

data class BaseErrorResponse(
    @SerializedName("statusCode")
    val statusCode: String?,
    @SerializedName("errorString")
    val error: String?,
) {
    fun toDomainEntity(): BaseErrorResponseDomainEntity {
        return BaseErrorResponseDomainEntity(
            errorString = error ?: "",
        )
    }
}