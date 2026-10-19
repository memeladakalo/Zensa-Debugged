package com.zensa.api.service

import com.zensa.api.dto.SyncOperationDto
import com.zensa.api.dto.SyncRequestDto
import com.zensa.api.dto.SyncResponseDto
import com.zensa.api.dto.SyncResultDto
import com.zensa.api.entity.AssignmentEntity
import com.zensa.api.entity.ModuleEntity
import com.zensa.api.entity.StudySessionEntity
import com.zensa.api.entity.SubtaskEntity
import com.zensa.api.entity.NoteEntity
import com.zensa.api.repository.NoteRepository
import com.zensa.api.repository.AssignmentRepository
import com.zensa.api.repository.ModuleRepository
import com.zensa.api.repository.StudySessionRepository
import com.zensa.api.repository.SubtaskRepository
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

/**
 * SyncService
 *
 * Applies a batch of queued operations sent by the Android client.
 *
 * Contract:
 *   - Operations are processed in the order they arrive.
 *   - Each operation is isolated: a failure on one does NOT roll
 *     back the others. The client receives a per-operation result.
 *   - A successful CREATE returns the server-generated id so the
 *     client can store it on the local row and mark it synced.
 *
 * Ownership: every operation is scoped to the authenticated user.
 * The caller passes userId; SyncService verifies ownership before
 * touching a row.
 */
@Service
class SyncService(
    private val moduleRepo: ModuleRepository,
    private val assignmentRepo: AssignmentRepository,
    private val subtaskRepo: SubtaskRepository,
    private val studySessionRepo: StudySessionRepository
    private val noteRepo: NoteRepository
) {

    private val json = Json { ignoreUnknownKeys = true }

    fun applyBatch(userId: UUID, request: SyncRequestDto): SyncResponseDto {
        val results = request.operations.map { op ->
            try {
                applyOne(userId, op)
            } catch (e: Exception) {
                SyncResultDto(
                    queueId = op.queueId,
                    success = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
        return SyncResponseDto(results = results)
    }

    // ---------------- Dispatcher ----------------

    private fun applyOne(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.entityType.uppercase()) {
            "MODULE"        -> applyModule(userId, op)
            "ASSIGNMENT"    -> applyAssignment(userId, op)
            "SUBTASK"       -> applySubtask(userId, op)
            "STUDY_SESSION" -> applyStudySession(userId, op)
            "NOTE" -> applyNote(userId, op)
            else -> SyncResultDto(
                queueId = op.queueId,
                success = false,
                error = "Unknown entityType: ${op.entityType}"
            )
        }
    }

    // ---------------- MODULE ----------------

    private fun applyModule(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.operation.uppercase()) {
            "CREATE" -> {
                val payload = parse(op.payload)
                val entity = ModuleEntity(
                    userId = userId,
                    moduleCode = payload.str("moduleCode").uppercase(),
                    name = payload.str("name"),
                    description = payload.str("description"),
                    colour = payload.str("colour").ifBlank { "#6A4BAF" }
                )
                val saved = moduleRepo.save(entity)
                SyncResultDto(op.queueId, true, serverId = saved.moduleId.toString())
            }
            "UPDATE" -> {
                val serverId = requireServerId(op)
                val existing = moduleRepo.findByModuleIdAndUserId(serverId, userId)
                    ?: return notFound(op, "Module")
                val payload = parse(op.payload)
                existing.moduleCode = payload.str("moduleCode").uppercase()
                existing.name = payload.str("name")
                existing.description = payload.str("description")
                existing.colour = payload.str("colour").ifBlank { existing.colour }
                existing.updatedAt = LocalDateTime.now()
                moduleRepo.save(existing)
                SyncResultDto(op.queueId, true, serverId = existing.moduleId.toString())
            }
            "DELETE" -> {
                val serverId = requireServerId(op)
                val existing = moduleRepo.findByModuleIdAndUserId(serverId, userId)
                    ?: return SyncResultDto(op.queueId, true, serverId = serverId.toString())
                moduleRepo.delete(existing)
                SyncResultDto(op.queueId, true, serverId = serverId.toString())
            }
            else -> unknownOperation(op)
        }
    }

    // ---------------- ASSIGNMENT ----------------

    private fun applyAssignment(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.operation.uppercase()) {
            "CREATE" -> {
                val payload = parse(op.payload)
                val entity = AssignmentEntity(
                    userId = userId,
                    moduleId = payload.strOrNull("moduleId")?.let(UUID::fromString),
                    title = payload.str("title"),
                    description = payload.str("description"),
                    priority = payload.str("priority").ifBlank { "MEDIUM" },
                    status = payload.str("status").ifBlank { "PENDING" },
                    dueDate = payload.str("dueDate"),
                    isCompleted = payload.bool("isCompleted")
                )
                val saved = assignmentRepo.save(entity)
                SyncResultDto(op.queueId, true, serverId = saved.assignmentId.toString())
            }
            "UPDATE" -> {
                val serverId = requireServerId(op)
                val existing = assignmentRepo.findByAssignmentIdAndUserId(serverId, userId)
                    ?: return notFound(op, "Assignment")
                val payload = parse(op.payload)
                existing.moduleId = payload.strOrNull("moduleId")?.let(UUID::fromString)
                existing.title = payload.str("title")
                existing.description = payload.str("description")
                existing.priority = payload.str("priority").ifBlank { existing.priority }
                existing.status = payload.str("status").ifBlank { existing.status }
                existing.dueDate = payload.str("dueDate")
                existing.isCompleted = payload.bool("isCompleted")
                existing.updatedAt = LocalDateTime.now()
                assignmentRepo.save(existing)
                SyncResultDto(op.queueId, true, serverId = existing.assignmentId.toString())
            }
            "DELETE" -> {
                val serverId = requireServerId(op)
                val existing = assignmentRepo.findByAssignmentIdAndUserId(serverId, userId)
                    ?: return SyncResultDto(op.queueId, true, serverId = serverId.toString())
                assignmentRepo.delete(existing)
                SyncResultDto(op.queueId, true, serverId = serverId.toString())
            }
            else -> unknownOperation(op)
        }
    }

    // ---------------- SUBTASK ----------------

    private fun applySubtask(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.operation.uppercase()) {
            "CREATE" -> {
                val payload = parse(op.payload)
                val parentServerId = payload.strOrNull("assignmentServerId")
                    ?: return SyncResultDto(
                        op.queueId, false,
                        error = "Subtask has no parent assignment on the server yet"
                    )
                val parent = assignmentRepo.findByAssignmentIdAndUserId(
                    UUID.fromString(parentServerId), userId
                ) ?: return notFound(op, "Parent assignment")

                val entity = SubtaskEntity(
                    assignmentId = parent.assignmentId,
                    title = payload.str("title"),
                    position = payload.int("position"),
                    isCompleted = payload.bool("isCompleted")
                )
                val saved = subtaskRepo.save(entity)
                SyncResultDto(op.queueId, true, serverId = saved.subtaskId.toString())
            }
            "UPDATE" -> {
                val serverId = requireServerId(op)
                val existing = subtaskRepo.findById(serverId).orElse(null)
                    ?: return notFound(op, "Subtask")
                // Ownership via parent
                val parentId = existing.assignmentId ?: return notFound(op, "Parent assignment")
                assignmentRepo.findByAssignmentIdAndUserId(parentId, userId)
                    ?: return notFound(op, "Subtask")

                val payload = parse(op.payload)
                existing.title = payload.str("title")
                existing.position = payload.int("position")
                existing.isCompleted = payload.bool("isCompleted")
                existing.updatedAt = LocalDateTime.now()
                subtaskRepo.save(existing)
                SyncResultDto(op.queueId, true, serverId = existing.subtaskId.toString())
            }
            "DELETE" -> {
                val serverId = requireServerId(op)
                val existing = subtaskRepo.findById(serverId).orElse(null)
                    ?: return SyncResultDto(op.queueId, true, serverId = serverId.toString())
                val parentId = existing.assignmentId ?: return notFound(op, "Parent assignment")
                assignmentRepo.findByAssignmentIdAndUserId(parentId, userId)
                    ?: return notFound(op, "Subtask")
                subtaskRepo.delete(existing)
                SyncResultDto(op.queueId, true, serverId = serverId.toString())
            }
            else -> unknownOperation(op)
        }
    }

    // ---------------- STUDY SESSION ----------------

    private fun applyStudySession(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.operation.uppercase()) {
            "CREATE" -> {
                val payload = parse(op.payload)
                val entity = StudySessionEntity(
                    userId = userId,
                    moduleId = payload.strOrNull("moduleId")?.let(UUID::fromString),
                    startedAt = payload.str("startedAt"),
                    endedAt = payload.strOrNull("endedAt"),
                    durationSeconds = payload.int("durationSeconds"),
                    completed = payload.bool("completed")
                )
                val saved = studySessionRepo.save(entity)
                SyncResultDto(op.queueId, true, serverId = saved.sessionId.toString())
            }
            "UPDATE" -> {
                // Not currently used by the client — kept for symmetry.
                SyncResultDto(op.queueId, true, serverId = op.serverId)
            }
            "DELETE" -> {
                val serverId = requireServerId(op)
                val existing = studySessionRepo.findBySessionIdAndUserId(serverId, userId)
                    ?: return SyncResultDto(op.queueId, true, serverId = serverId.toString())
                studySessionRepo.delete(existing)
                SyncResultDto(op.queueId, true, serverId = serverId.toString())
            }
            else -> unknownOperation(op)
        }
    }

        // ---------------- NOTE ----------------

    private fun applyNote(userId: UUID, op: SyncOperationDto): SyncResultDto {
        return when (op.operation.uppercase()) {
            "CREATE" -> {
                val payload = parse(op.payload)
                val entity = NoteEntity(
                    userId = userId,
                    moduleId = payload.strOrNull("moduleId")?.let(UUID::fromString),
                    title = payload.str("title"),
                    content = payload.str("content"),
                    attachmentUrl = payload.strOrNull("attachmentUrl")
                )
                val saved = noteRepo.save(entity)
                SyncResultDto(op.queueId, true, serverId = saved.noteId.toString())
            }
            "UPDATE" -> {
                val serverId = requireServerId(op)
                val existing = noteRepo.findByNoteIdAndUserId(serverId, userId)
                    ?: return notFound(op, "Note")
                val payload = parse(op.payload)
                existing.moduleId = payload.strOrNull("moduleId")?.let(UUID::fromString)
                existing.title = payload.str("title")
                existing.content = payload.str("content")
                existing.attachmentUrl = payload.strOrNull("attachmentUrl")
                existing.updatedAt = LocalDateTime.now()
                noteRepo.save(existing)
                SyncResultDto(op.queueId, true, serverId = existing.noteId.toString())
            }
            "DELETE" -> {
                val serverId = requireServerId(op)
                val existing = noteRepo.findByNoteIdAndUserId(serverId, userId)
                    ?: return SyncResultDto(op.queueId, true, serverId = serverId.toString())
                noteRepo.delete(existing)
                SyncResultDto(op.queueId, true, serverId = serverId.toString())
            }
            else -> unknownOperation(op)
        }
    }
    
    // ---------------- Helpers ----------------

    private fun parse(payload: String?): JsonObject {
        if (payload.isNullOrBlank()) return JsonObject(emptyMap())
        return json.parseToJsonElement(payload).let { it as JsonObject }
    }

    private fun JsonObject.str(key: String): String =
        this[key]?.jsonPrimitive?.content ?: ""

    private fun JsonObject.strOrNull(key: String): String? {
        val v = this[key]?.jsonPrimitive?.content
        return if (v.isNullOrBlank() || v == "null") null else v
    }

    private fun JsonObject.int(key: String): Int =
        this[key]?.jsonPrimitive?.intOrNull ?: 0

    private fun JsonObject.bool(key: String): Boolean =
        this[key]?.jsonPrimitive?.booleanOrNull ?: false

    private fun requireServerId(op: SyncOperationDto): UUID =
        op.serverId?.let(UUID::fromString)
            ?: throw IllegalArgumentException("serverId is required for ${op.operation}")

    private fun notFound(op: SyncOperationDto, entity: String) =
        SyncResultDto(op.queueId, false, error = "$entity not found")

    private fun unknownOperation(op: SyncOperationDto) =
        SyncResultDto(op.queueId, false, error = "Unknown operation: ${op.operation}")
}
