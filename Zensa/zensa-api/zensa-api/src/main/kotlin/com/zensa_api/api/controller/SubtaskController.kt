package com.zensa.api.controller

import com.zensa.api.dto.SubtaskRequest
import com.zensa.api.dto.SubtaskResponse
import com.zensa.api.service.SubtaskService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Subtask endpoints.
 *
 * Endpoints are split across two base paths:
 *   /api/assignments/{assignmentId}/subtasks   (list + create)
 *   /api/subtasks/{id}                         (update + delete)
 *
 * This mirrors the REST conventions used by the Android client.
 */
@RestController
class SubtaskController(
    private val subtaskService: SubtaskService
) {

    // ---------- Nested under an assignment ----------

    @GetMapping("/api/assignments/{assignmentId}/subtasks")
    fun list(
        @PathVariable assignmentId: String
    ): ResponseEntity<List<SubtaskResponse>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            subtaskService.listForAssignment(userId, UUID.fromString(assignmentId))
        )
    }

    @PostMapping("/api/assignments/{assignmentId}/subtasks")
    fun create(
        @PathVariable assignmentId: String,
        @Valid @RequestBody request: SubtaskRequest
    ): ResponseEntity<SubtaskResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.status(HttpStatus.CREATED).body(
            subtaskService.create(userId, UUID.fromString(assignmentId), request)
        )
    }

    // ---------- Direct by subtask id ----------

    @PutMapping("/api/subtasks/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: SubtaskRequest
    ): ResponseEntity<SubtaskResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            subtaskService.update(userId, UUID.fromString(id), request)
        )
    }

    @DeleteMapping("/api/subtasks/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        subtaskService.delete(userId, UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
