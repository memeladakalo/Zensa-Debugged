package com.zensa.api.repository

import com.zensa.api.entity.AssignmentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AssignmentRepository : JpaRepository<AssignmentEntity, UUID> {

    fun findAllByUserIdOrderByDueDateAsc(userId: UUID): List<AssignmentEntity>

    fun findAllByUserIdAndModuleIdOrderByDueDateAsc(
        userId: UUID,
        moduleId: UUID
    ): List<AssignmentEntity>

    fun findByAssignmentIdAndUserId(
        assignmentId: UUID,
        userId: UUID
    ): AssignmentEntity?
}
