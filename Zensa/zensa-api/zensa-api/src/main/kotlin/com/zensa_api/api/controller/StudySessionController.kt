package com.zensa.api.controller

import com.zensa.api.dto.StudySessionRequest
import com.zensa.api.dto.StudySessionResponse
import com.zensa.api.service.StudySessionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/study-sessions")
class StudySessionController(
    private val studySessionService: StudySessionService
) {

    @GetMapping
    fun list(): ResponseEntity<List<StudySessionResponse>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(studySessionService.listForUser(userId))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: StudySessionRequest
    ): ResponseEntity<StudySessionResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(studySessionService.create(userId, request))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        studySessionService.delete(userId, UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
