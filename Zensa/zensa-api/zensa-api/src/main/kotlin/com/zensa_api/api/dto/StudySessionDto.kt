package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero

data class StudySessionRequest(
    val moduleId: String? = null,

    @field:NotBlank(message = "startedAt is required")
    val startedAt: String = "",

    val endedAt: String? = null,

    @field:PositiveOrZero(message = "durationSeconds must be zero or positive")
    val durationSeconds: Int = 0,

    val completed: Boolean = false
)

data class StudySessionResponse(
    val sessionId: String,
    val userId: String,
    val moduleId: String?,
    val startedAt: String,
    val endedAt: String?,
    val durationSeconds: Int,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String
)
