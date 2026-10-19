package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * DTOs matching the Spring Boot /api/auth JSON contract exactly.
 * If the backend field names change, these must change too.
 */
@Serializable
data class RegisterRequestDto(
    val name: String,
    val email: String,
    val password: String
)

@Serializable
data class RegisterResponseDto(
    val userId: String,
    val name: String,
    val email: String,
    val message: String
)

@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val userId: String,
    val name: String,
    val email: String
)

@Serializable
data class UserProfileDto(
    val userId: String,
    val name: String,
    val email: String
)

@Serializable
data class ApiErrorDto(
    val status: Int = 0,
    val message: String = "Uknown Error")
