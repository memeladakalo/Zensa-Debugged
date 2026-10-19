package com.zensa.app.data.mapper

import com.zensa.app.data.entity.AssignmentEntity
import com.zensa.app.data.remote.dto.AssignmentDto
import com.zensa.app.data.remote.dto.AssignmentRequestDto
import java.util.UUID

/**
 * AssignmentMapper — converts between the server DTO and the local
 * Room entity. Keeping this in one file means if the server DTO
 * changes, only this file needs updating.
 */
object AssignmentMapper {

    /**
     * Server DTO → local Room entity.
     *
     * moduleId is the SERVER module UUID. To resolve it to the
     * local module row we'd need a lookup — that's done at the
     * repository level where we have access to the ModuleDao.
     * Here we simply leave moduleLocalId null and let the
     * repository fill it in.
     */
    fun fromDto(
        dto: AssignmentDto,
        localId: String = dto.assignmentId,
        moduleLocalId: String? = null
    ): AssignmentEntity = AssignmentEntity(
        localId = localId,
        serverId = dto.assignmentId,
        userId = dto.userId,
        moduleLocalId = moduleLocalId,
        title = dto.title,
        description = dto.description,
        priority = dto.priority,
        status = dto.status,
        dueDate = dto.dueDate,
        isCompleted = dto.isCompleted,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        isSynced = true,
        isDeleted = false
    )

    /**
     * Local entity → request body for the server.
     * moduleServerId must be resolved by the caller (usually the
     * repository, from ModuleDao.getByLocalId(...)?.serverId).
     */
    fun toRequestDto(
        entity: AssignmentEntity,
        moduleServerId: String?
    ): AssignmentRequestDto = AssignmentRequestDto(
        moduleId = moduleServerId,
        title = entity.title,
        description = entity.description,
        priority = entity.priority,
        status = entity.status,
        dueDate = entity.dueDate,
        isCompleted = entity.isCompleted
    )

    /** Generate a fresh local ID for offline creation. */
    fun newLocalId(): String = UUID.randomUUID().toString()
}
