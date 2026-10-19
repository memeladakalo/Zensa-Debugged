package com.zensa.api.service

import com.zensa.api.dto.NoteRequest
import com.zensa.api.dto.NoteResponse
import com.zensa.api.entity.NoteEntity
import com.zensa.api.repository.NoteRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class NoteService(
    private val noteRepository: NoteRepository
) {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listForUser(userId: UUID): List<NoteResponse> =
        noteRepository.findAllByUserIdOrderByUpdatedAtDesc(userId)
            .map { it.toResponse() }

    fun create(userId: UUID, request: NoteRequest): NoteResponse {
        val entity = NoteEntity(
            userId = userId,
            moduleId = request.moduleId?.let(UUID::fromString),
            title = request.title.trim(),
            content = request.content,
            attachmentUrl = request.attachmentUrl
        )
        return noteRepository.save(entity).toResponse()
    }

    fun update(
        userId: UUID,
        noteId: UUID,
        request: NoteRequest
    ): NoteResponse {
        val entity = noteRepository.findByNoteIdAndUserId(noteId, userId)
            ?: throw IllegalArgumentException("Note not found")

        entity.moduleId = request.moduleId?.let(UUID::fromString)
        entity.title = request.title.trim()
        entity.content = request.content
        entity.attachmentUrl = request.attachmentUrl
        entity.updatedAt = LocalDateTime.now()

        return noteRepository.save(entity).toResponse()
    }

    fun delete(userId: UUID, noteId: UUID) {
        val entity = noteRepository.findByNoteIdAndUserId(noteId, userId)
            ?: throw IllegalArgumentException("Note not found")
        noteRepository.delete(entity)
    }

    private fun NoteEntity.toResponse() = NoteResponse(
        noteId = noteId.toString(),
        userId = userId.toString(),
        moduleId = moduleId?.toString(),
        title = title,
        content = content,
        attachmentUrl = attachmentUrl,
        createdAt = createdAt.format(iso),
        updatedAt = updatedAt.format(iso)
    )
}
