package com.zensa.api.service

import com.zensa.api.dto.SubtaskRequest
import com.zensa.api.dto.SubtaskResponse
import com.zensa.api.entity.SubtaskEntity
import com.zensa.api.repository.AssignmentRepository
import com.zensa.api.repository.SubtaskRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class SubtaskService(
    private val subtaskRepository: SubtaskRepository,
    private val assignmentRepository: AssignmentRepository
) {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    /**
     * List every subtask for one assignment, provided the caller
     * owns that assignment.
     */
    fun listForAssignment(userId: UUID, assignmentId: UUID): List<SubtaskResponse> {
        // Ownership check: the assignment must belong to this user.
        assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Assignment not found")

        return subtaskRepository
            .findAllByAssignmentIdOrderByPositionAscCreatedAtAsc(assignmentId)
            .map { it.toResponse() }
    }

    fun create(
        userId: UUID,
        assignmentId: UUID,
        request: SubtaskRequest
    ): SubtaskResponse {
        assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Assignment not found")

        val entity = SubtaskEntity(
            assignmentId = assignmentId,
            title = request.title.trim(),
            position = request.position,
            isCompleted = request.isCompleted
        )
        return subtaskRepository.save(entity).toResponse()
    }

    fun update(
        userId: UUID,
        subtaskId: UUID,
        request: SubtaskRequest
    ): SubtaskResponse {
        val entity = subtaskRepository.findById(subtaskId).orElse(null)
            ?: throw IllegalArgumentException("Subtask not found")

        val assignmentId = entity.assignmentId
            ?: throw IllegalArgumentException("Subtask has no assignment")

        // Ownership check via the parent assignment.
        assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Subtask not found")

        entity.title = request.title.trim()
        entity.position = request.position
        entity.isCompleted = request.isCompleted
        entity.updatedAt = LocalDateTime.now()

        return subtaskRepository.save(entity).toResponse()
    }

    fun delete(userId: UUID, subtaskId: UUID) {
        val entity = subtaskRepository.findById(subtaskId).orElse(null)
            ?: throw IllegalArgumentException("Subtask not found")

        val assignmentId = entity.assignmentId
            ?: throw IllegalArgumentException("Subtask has no assignment")

        assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Subtask not found")

        subtaskRepository.delete(entity)
    }

    private fun SubtaskEntity.toResponse() = SubtaskResponse(
        subtaskId = subtaskId.toString(),
        assignmentId = assignmentId.toString(),
        title = title,
        position = position,
        isCompleted = isCompleted,
        createdAt = createdAt.format(iso),
        updatedAt = updatedAt.format(iso)
    )
}
