package com.zensa.api.service

import com.zensa.api.dto.AssignmentRequest
import com.zensa.api.dto.AssignmentResponse
import com.zensa.api.entity.AssignmentEntity
import com.zensa.api.repository.AssignmentRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class AssignmentService(
    private val assignmentRepository: AssignmentRepository
) {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listForUser(userId: UUID): List<AssignmentResponse> =
        assignmentRepository.findAllByUserIdOrderByDueDateAsc(userId).map { it.toResponse() }

    fun create(userId: UUID, request: AssignmentRequest): AssignmentResponse {
        val entity = AssignmentEntity(
            userId = userId,
            moduleId = request.moduleId?.let { UUID.fromString(it) },
            title = request.title.trim(),
            description = request.description.trim(),
            priority = request.priority,
            status = request.status,
            dueDate = request.dueDate,
            isCompleted = request.isCompleted
        )
        return assignmentRepository.save(entity).toResponse()
    }

    fun update(
        userId: UUID,
        assignmentId: UUID,
        request: AssignmentRequest
    ): AssignmentResponse {
        val entity = assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Assignment not found")

        entity.moduleId = request.moduleId?.let { UUID.fromString(it) }
        entity.title = request.title.trim()
        entity.description = request.description.trim()
        entity.priority = request.priority
        entity.status = request.status
        entity.dueDate = request.dueDate
        entity.isCompleted = request.isCompleted
        entity.updatedAt = LocalDateTime.now()

        return assignmentRepository.save(entity).toResponse()
    }

    fun delete(userId: UUID, assignmentId: UUID) {
        val entity = assignmentRepository.findByAssignmentIdAndUserId(assignmentId, userId)
            ?: throw IllegalArgumentException("Assignment not found")
        assignmentRepository.delete(entity)
    }

    private fun AssignmentEntity.toResponse() = AssignmentResponse(
        assignmentId = assignmentId.toString(),
        userId = userId.toString(),
        moduleId = moduleId?.toString(),
        title = title,
        description = description,
        priority = priority,
        status = status,
        dueDate = dueDate,
        isCompleted = isCompleted,
        createdAt = createdAt.format(iso),
        updatedAt = updatedAt.format(iso)
    )
}
