package com.zensa.api.controller

import com.zensa.api.dto.NoteRequest
import com.zensa.api.dto.NoteResponse
import com.zensa.api.service.NoteService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/notes")
class NoteController(
    private val noteService: NoteService
) {

    @GetMapping
    fun list(): ResponseEntity<List<NoteResponse>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(noteService.listForUser(userId))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: NoteRequest
    ): ResponseEntity<NoteResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(noteService.create(userId, request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: NoteRequest
    ): ResponseEntity<NoteResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(
            noteService.update(userId, UUID.fromString(id), request)
        )
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        noteService.delete(userId, UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
