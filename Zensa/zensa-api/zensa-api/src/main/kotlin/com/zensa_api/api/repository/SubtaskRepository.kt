package com.zensa.api.repository

import com.zensa.api.entity.SubtaskEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface SubtaskRepository : JpaRepository<SubtaskEntity, UUID> {

    fun findAllByAssignmentIdOrderByPositionAscCreatedAtAsc(
        assignmentId: UUID
    ): List<SubtaskEntity>

    fun findBySubtaskIdAndAssignmentId(
        subtaskId: UUID,
        assignmentId: UUID
    ): SubtaskEntity?

    fun deleteAllByAssignmentId(assignmentId: UUID)
}
