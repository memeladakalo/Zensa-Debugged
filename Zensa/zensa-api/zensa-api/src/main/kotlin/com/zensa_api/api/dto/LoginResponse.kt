package com.zensa.api.dto

data class LoginResponse(
    val token: String,
    val userId: String,
    val name: String,
    val email: String
)
