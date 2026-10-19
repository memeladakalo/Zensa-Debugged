package com.zensa.app.data.mapper

import com.zensa.app.data.entity.StudySessionEntity
import com.zensa.app.data.remote.dto.StudySessionDto
import com.zensa.app.data.remote.dto.StudySessionRequestDto
import java.util.UUID

/**
 * StudySessionMapper — converts between server DTO and Room entity.
 */
object StudySessionMapper {

    fun fromDto(
        dto: StudySessionDto,
        localId: String = dto.sessionId,
        moduleLocalId: String? = null
    ): StudySessionEntity = StudySessionEntity(
        localId = localId,
        serverId = dto.sessionId,
        userId = dto.userId,
        moduleLocalId = moduleLocalId,
        startedAt = dto.startedAt,
        endedAt = dto.endedAt,
        durationSeconds = dto.durationSeconds,
        completed = dto.completed,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        isSynced = true,
        isDeleted = false
    )

    fun toRequestDto(
        entity: StudySessionEntity,
        moduleServerId: String?
    ): StudySessionRequestDto = StudySessionRequestDto(
        moduleId = moduleServerId,
        startedAt = entity.startedAt,
        endedAt = entity.endedAt,
        durationSeconds = entity.durationSeconds,
        completed = entity.completed
    )

    fun newLocalId(): String = UUID.randomUUID().toString()
}
