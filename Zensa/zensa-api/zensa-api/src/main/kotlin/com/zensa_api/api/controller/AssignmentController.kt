package com.zensa.api.controller

import com.zensa.api.dto.AssignmentRequest
import com.zensa.api.dto.AssignmentResponse
import com.zensa.api.service.AssignmentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/assignments")
class AssignmentController(
    private val assignmentService: AssignmentService
) {

    @GetMapping
    fun list(): ResponseEntity<List<AssignmentResponse>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(assignmentService.listForUser(userId))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: AssignmentRequest
    ): ResponseEntity<AssignmentResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(assignmentService.create(userId, request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: AssignmentRequest
    ): ResponseEntity<AssignmentResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            assignmentService.update(userId, UUID.fromString(id), request)
        )
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        assignmentService.delete(userId, UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
