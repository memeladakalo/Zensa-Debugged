package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * One queued operation sent to POST /api/sync.
 *
 * The server applies each operation in order and returns a
 * per-operation result. Field names match the Spring Boot DTO.
 */



@Serializable
data class SyncRequestDto(
    val operations: List<SyncOperationDto>
)

@Serializable
data class SyncResultDto(
    val queueId: String,
    val success: Boolean,
    val serverId: String? = null,
    val error: String? = null
)

@Serializable
data class SyncResponseDto(
    val results: List<SyncResultDto>
)
