package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Server-side Subtask entity.
 *
 * assignmentId is a plain UUID column (not @ManyToOne), matching
 * the style used for AssignmentEntity.moduleId. This keeps the
 * mapping explicit and easy to reason about.
 */
@Entity
@Table(name = "subtasks")
class SubtaskEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subtask_id", updatable = false, nullable = false)
    var subtaskId: UUID? = null,

    @Column(name = "assignment_id", nullable = false)
    var assignmentId: UUID? = null,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(nullable = false)
    var position: Int = 0,

    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
