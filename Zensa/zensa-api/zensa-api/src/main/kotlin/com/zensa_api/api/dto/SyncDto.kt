package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

/**
 * One queued operation sent by the Android app's sync worker.
 *
 * The Android client sends these in FIFO order. The server applies
 * them one at a time so that a MODULE CREATE completes before any
 * ASSIGNMENT CREATE that references it.
 */
data class SyncOperationDto(

    @field:NotBlank(message = "queueId is required")
    val queueId: String = "",

    @field:NotBlank(message = "entityType is required")
    val entityType: String = "",   // MODULE / ASSIGNMENT / SUBTASK / STUDY_SESSION

    @field:NotBlank(message = "entityLocalId is required")
    val entityLocalId: String = "",

    /** Server UUID if the row already exists on the server. */
    val serverId: String? = null,

    @field:NotBlank(message = "operation is required")
    val operation: String = "",    // CREATE / UPDATE / DELETE

    /**
     * JSON string carrying the row's fields. Null for DELETE.
     * Its shape depends on entityType:
     *   MODULE         -> ModulePayload
     *   ASSIGNMENT     -> AssignmentPayload
     *   SUBTASK        -> SubtaskPayload
     *   STUDY_SESSION  -> StudySessionPayload
     */
    val payload: String? = null
)

data class SyncRequestDto(
    @field:NotEmpty(message = "operations must not be empty")
    val operations: List<SyncOperationDto> = emptyList()
)

data class SyncResultDto(
    val queueId: String,
    val success: Boolean,
    val serverId: String? = null,
    val error: String? = null
)

data class SyncResponseDto(
    val results: List<SyncResultDto> = emptyList()
)
