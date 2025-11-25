package com.ptit.iot

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT

interface MainApi {
    @POST("/auth/register")
    suspend fun register(
        @Body registerRequest: LoginRegisterRequest
    ): Resource<LoginRegisterResponse>

    @POST("/auth/login")
    suspend fun login(
        @Body loginRequest: LoginRegisterRequest
    ): Resource<LoginRegisterResponse>

    @GET("/realtime-heart")
    suspend fun getRealtimeHeart(): Resource<HeartRateResponse>

    @GET("/profile")
    suspend fun getProfile(): Resource<ProfileResponse>

    @PUT("/profile")
    suspend fun updateProfile(
        @Body updateProfileRequest: UpdateProfileRequest
    ): Resource<Unit>
}