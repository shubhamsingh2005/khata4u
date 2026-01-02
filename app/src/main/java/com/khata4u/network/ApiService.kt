package com.khata4u.network

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/api/auth/login")
    fun login(@Body req: LoginRequest): Call<LoginResponse>

    @POST("/api/auth/refresh")
    fun refresh(@Body req: RefreshRequest): Call<LoginResponse>

    @POST("/api/auth/register")
    fun register(@Body req: RegisterRequest): Call<LoginResponse>
}
