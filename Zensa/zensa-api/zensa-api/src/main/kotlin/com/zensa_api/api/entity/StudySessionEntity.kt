package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "study_sessions")
class StudySessionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "session_id", updatable = false, nullable = false)
    var sessionId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "module_id")
    var moduleId: UUID? = null,

    @Column(name = "started_at", nullable = false, length = 40)
    var startedAt: String = "",

    @Column(name = "ended_at", length = 40)
    var endedAt: String? = null,

    @Column(name = "duration_seconds", nullable = false)
    var durationSeconds: Int = 0,

    @Column(nullable = false)
    var completed: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
