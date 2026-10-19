package com.zensa.api.dto

import java.util.UUID

/**
 * Response body for POST /api/auth/register.
 *
 * IMPORTANT: never include the password or password hash here.
 */
data class RegisterResponse(
    val userId: String,
    val name: String,
    val email: String,
    val message: String = "Registration successful"
)
