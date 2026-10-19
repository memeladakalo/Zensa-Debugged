package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class AssignmentRequest(
    val moduleId: String? = null,

    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title is too long")
    val title: String = "",

    @field:Size(max = 2000, message = "Description is too long")
    val description: String = "",

    val priority: String = "MEDIUM",
    val status: String = "PENDING",

    @field:NotBlank(message = "Due date is required")
    val dueDate: String = "",

    val isCompleted: Boolean = false
)

data class AssignmentResponse(
    val assignmentId: String,
    val userId: String,
    val moduleId: String?,
    val title: String,
    val description: String,
    val priority: String,
    val status: String,
    val dueDate: String,
    val isCompleted: Boolean,
    val createdAt: String,
    val updatedAt: String
)
