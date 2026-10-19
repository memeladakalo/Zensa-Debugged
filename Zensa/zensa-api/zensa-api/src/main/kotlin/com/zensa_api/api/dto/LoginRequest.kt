package com.zensa.api.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Enter a valid email address")
    val email: String = "",

    @field:NotBlank(message = "Password is required")
    val password: String = ""
)
