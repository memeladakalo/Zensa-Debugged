package com.zensa.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * Request body for POST /api/auth/register.
 *
 * Jakarta validation annotations are enforced by Spring
 * automatically when the controller uses @Valid.
 */
data class RegisterRequest(

    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name is too long")
    val name: String = "",

    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Enter a valid email address")
    @field:Size(max = 150, message = "Email is too long")
    val email: String = "",

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String = ""
)