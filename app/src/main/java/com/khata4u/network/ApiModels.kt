package com.khata4u.network

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class RefreshRequest(
    val refreshToken: String
)

// Register request for signup
data class RegisterRequest(
    val name: String,
    val email: String,
    val phone: String?,
    val password: String,
    @SerializedName("role")
    val userType: String
)

data class LoginResponse(
    val token: String,
    val refreshToken: String? = null,
    val expiresIn: Long? = null // seconds
)
