package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response from the server for one study session.
 */
@Serializable
data class StudySessionDto(
    val sessionId: String,
    val userId: String,
    val moduleId: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val durationSeconds: Int,
    val completed: Boolean,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request body when creating or updating a study session.
 * moduleId refers to the SERVER module UUID (nullable).
 */
@Serializable
data class StudySessionRequestDto(
    val moduleId: String? = null,
    val startedAt: String,
    val endedAt: String? = null,
    val durationSeconds: Int,
    val completed: Boolean
)
