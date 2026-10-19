package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Server-side Assignment entity. Mirrors the Android Room entity.
 *
 * moduleId is a foreign key to modules.module_id, but declared as a
 * plain UUID column (not @ManyToOne) so we control the joins
 * explicitly and avoid lazy-loading surprises.
 */
@Entity
@Table(name = "assignments")
class AssignmentEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "assignment_id", updatable = false, nullable = false)
    var assignmentId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "module_id")
    var moduleId: UUID? = null,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Column(length = 2000)
    var description: String = "",

    @Column(nullable = false, length = 20)
    var priority: String = "MEDIUM",

    @Column(nullable = false, length = 20)
    var status: String = "PENDING",

    @Column(name = "due_date", nullable = false, length = 40)
    var dueDate: String = "",

    @Column(name = "is_completed", nullable = false)
    var isCompleted: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
