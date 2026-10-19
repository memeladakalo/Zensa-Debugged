package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Response body from the server for an assignment.
 * Field names must match the Spring Boot JSON exactly.
 */
@Serializable
data class AssignmentDto(
    val assignmentId: String,
    val userId: String,
    val moduleId: String? = null,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM",
    val status: String = "PENDING",
    val dueDate: String,
    val isCompleted: Boolean = false,
    val createdAt: String,
    val updatedAt: String
)

/**
 * Request body sent to the server when creating or updating
 * an assignment.
 *
 * Note: moduleId refers to the SERVER's module UUID. When the
 * module was created offline and has no serverId yet, this field
 * is null. The sync worker resolves it once the module is synced.
 */
@Serializable
data class AssignmentRequestDto(
    val moduleId: String? = null,
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM",
    val status: String = "PENDING",
    val dueDate: String,
    val isCompleted: Boolean = false
)
