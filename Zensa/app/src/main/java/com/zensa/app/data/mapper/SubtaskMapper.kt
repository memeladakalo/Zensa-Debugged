package com.zensa.app.data.mapper

import com.zensa.app.data.entity.SubtaskEntity
import com.zensa.app.data.remote.dto.SubtaskDto
import com.zensa.app.data.remote.dto.SubtaskRequestDto
import java.util.UUID

/**
 * SubtaskMapper — converts between the server DTO and the local
 * Room entity.
 */
object SubtaskMapper {

    /**
     * Server DTO → local Room entity.
     * assignmentLocalId must be supplied by the caller — the DTO
     * only carries the SERVER assignment UUID.
     */
    fun fromDto(
        dto: SubtaskDto,
        assignmentLocalId: String,
        localId: String = dto.subtaskId
    ): SubtaskEntity = SubtaskEntity(
        localId = localId,
        serverId = dto.subtaskId,
        assignmentLocalId = assignmentLocalId,
        title = dto.title,
        position = dto.position,
        isCompleted = dto.isCompleted,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        isSynced = true,
        isDeleted = false
    )

    /** Local entity → request body for the server. */
    fun toRequestDto(entity: SubtaskEntity): SubtaskRequestDto =
        SubtaskRequestDto(
            title = entity.title,
            position = entity.position,
            isCompleted = entity.isCompleted
        )

    /** Generate a fresh local ID for offline creation. */
    fun newLocalId(): String = UUID.randomUUID().toString()
}
