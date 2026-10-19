package com.zensa.api.repository

import com.zensa.api.entity.StudySessionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface StudySessionRepository : JpaRepository<StudySessionEntity, UUID> {

    fun findAllByUserIdOrderByStartedAtDesc(userId: UUID): List<StudySessionEntity>

    fun findBySessionIdAndUserId(
        sessionId: UUID,
        userId: UUID
    ): StudySessionEntity?
}
