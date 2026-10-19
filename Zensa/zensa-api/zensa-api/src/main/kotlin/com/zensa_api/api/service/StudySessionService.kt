package com.zensa.api.service

import com.zensa.api.dto.StudySessionRequest
import com.zensa.api.dto.StudySessionResponse
import com.zensa.api.entity.StudySessionEntity
import com.zensa.api.repository.StudySessionRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class StudySessionService(
    private val studySessionRepository: StudySessionRepository
) {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listForUser(userId: UUID): List<StudySessionResponse> =
        studySessionRepository.findAllByUserIdOrderByStartedAtDesc(userId)
            .map { it.toResponse() }

    fun create(userId: UUID, request: StudySessionRequest): StudySessionResponse {
        val entity = StudySessionEntity(
            userId = userId,
            moduleId = request.moduleId?.let { UUID.fromString(it) },
            startedAt = request.startedAt,
            endedAt = request.endedAt,
            durationSeconds = request.durationSeconds,
            completed = request.completed
        )
        return studySessionRepository.save(entity).toResponse()
    }

    fun delete(userId: UUID, sessionId: UUID) {
        val entity = studySessionRepository.findBySessionIdAndUserId(sessionId, userId)
            ?: throw IllegalArgumentException("Session not found")
        studySessionRepository.delete(entity)
    }

    private fun StudySessionEntity.toResponse() = StudySessionResponse(
        sessionId = sessionId.toString(),
        userId = userId.toString(),
        moduleId = moduleId?.toString(),
        startedAt = startedAt,
        endedAt = endedAt,
        durationSeconds = durationSeconds,
        completed = completed,
        createdAt = createdAt.format(iso),
        updatedAt = updatedAt.format(iso)
    )
}
