package com.zensa.app.data.mapper

import com.zensa.app.data.entity.NoteEntity
import com.zensa.app.data.remote.dto.NoteDto
import com.zensa.app.data.remote.dto.NoteRequestDto
import java.util.UUID

object NoteMapper {

    fun fromDto(
        dto: NoteDto,
        localId: String = dto.noteId,
        moduleLocalId: String? = null
    ): NoteEntity = NoteEntity(
        localId = localId,
        serverId = dto.noteId,
        userId = dto.userId,
        moduleLocalId = moduleLocalId,
        title = dto.title,
        content = dto.content,
        attachmentUrl = dto.attachmentUrl,
        createdAt = dto.createdAt,
        updatedAt = dto.updatedAt,
        isSynced = true,
        isDeleted = false
    )

    fun toRequestDto(
        entity: NoteEntity,
        moduleServerId: String?
    ): NoteRequestDto = NoteRequestDto(
        moduleId = moduleServerId,
        title = entity.title,
        content = entity.content,
        attachmentUrl = entity.attachmentUrl
    )

    fun newLocalId(): String = UUID.randomUUID().toString()
}
