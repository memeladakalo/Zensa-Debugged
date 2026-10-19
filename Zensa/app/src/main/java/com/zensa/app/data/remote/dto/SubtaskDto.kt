package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response body from the server for a subtask.
 */
@Serializable
data class SubtaskDto(
    val subtaskId: String,
    val assignmentId: String,
    val title: String,
    val position: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request body when creating or updating a subtask.
 * assignmentId refers to the server's assignment UUID.
 */
@Serializable
data class SubtaskRequestDto(
    val title: String,
    val position: Int = 0,
    val isCompleted: Boolean = false
)
