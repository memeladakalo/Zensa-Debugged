package com.zensa.api.repository

import com.zensa.api.entity.NoteEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface NoteRepository : JpaRepository<NoteEntity, UUID> {

    fun findAllByUserIdOrderByUpdatedAtDesc(userId: UUID): List<NoteEntity>

    fun findAllByUserIdAndModuleIdOrderByUpdatedAtDesc(
        userId: UUID,
        moduleId: UUID
    ): List<NoteEntity>

    fun findByNoteIdAndUserId(
        noteId: UUID,
        userId: UUID
    ): NoteEntity?
}
