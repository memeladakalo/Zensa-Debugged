package com.zensa.app.data.mapper

import com.zensa.app.data.entity.ModuleEntity
import com.zensa.app.data.remote.dto.ModuleDto
import java.util.UUID

/**
 * Converts between Room entity, REST DTO, and the local model.
 * Keeping this in one place means if the server DTO changes,
 * only this file needs updating.
 */
object ModuleMapper {

    /** Server DTO → local Room entity. */
    fun fromDto(dto: ModuleDto, localId: String = dto.moduleId): ModuleEntity =
        ModuleEntity(
            localId = localId,
            serverId = dto.moduleId,
            userId = dto.userId,
            moduleCode = dto.moduleCode,
            name = dto.name,
            description = dto.description,
            colour = dto.colour,
            createdAt = dto.createdAt,
            updatedAt = dto.updatedAt,
            isSynced = true,
            isDeleted = false
        )

    /** Generate a fresh localId for offline creation. */
    fun newLocalId(): String = UUID.randomUUID().toString()
}
