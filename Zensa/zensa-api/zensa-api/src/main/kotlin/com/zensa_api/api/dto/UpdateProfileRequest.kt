package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpdateProfileRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 100, message = "Name is too long")
    val name: String = ""
)
