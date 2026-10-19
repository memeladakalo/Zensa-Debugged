package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class ModuleRequest(
    @field:NotBlank(message = "Module code is required")
    @field:Size(max = 20, message = "Module code is too long")
    val moduleCode: String = "",

    @field:NotBlank(message = "Module name is required")
    @field:Size(max = 100, message = "Module name is too long")
    val name: String = "",

    @field:Size(max = 500, message = "Description is too long")
    val description: String = "",

    val colour: String = "#6A4BAF"
)

data class ModuleResponse(
    val moduleId: String,
    val userId: String,
    val moduleCode: String,
    val name: String,
    val description: String,
    val colour: String,
    val createdAt: String,
    val updatedAt: String
)
