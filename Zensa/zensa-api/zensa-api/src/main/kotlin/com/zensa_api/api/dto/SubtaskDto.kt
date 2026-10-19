package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class SubtaskRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title is too long")
    val title: String = "",

    val position: Int = 0,

    val isCompleted: Boolean = false
)

data class SubtaskResponse(
    val subtaskId: String,
    val assignmentId: String,
    val title: String,
    val position: Int,
    val isCompleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)
