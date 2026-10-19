package com.zensa.api.controller

import com.zensa.api.dto.ModuleRequest
import com.zensa.api.dto.ModuleResponse
import com.zensa.api.service.ModuleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

/**
 * Module CRUD endpoints. All require a valid JWT.
 * The userId comes from the JWT filter — never from the request body.
 */
@RestController
@RequestMapping("/api/modules")
class ModuleController(
    private val moduleService: ModuleService
) {

    @GetMapping
    fun list(): ResponseEntity<List<ModuleResponse>> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(moduleService.listForUser(userId))
    }

    @PostMapping
    fun create(
        @Valid @RequestBody request: ModuleRequest
    ): ResponseEntity<ModuleResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.status(HttpStatus.CREATED).body(moduleService.create(userId, request))
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: ModuleRequest
    ): ResponseEntity<ModuleResponse> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(moduleService.update(userId, UUID.fromString(id), request))
    }

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: String): ResponseEntity<Void> {
        val userId = currentUserId() ?: return ResponseEntity.status(401).build()
        moduleService.delete(userId, UUID.fromString(id))
        return ResponseEntity.noContent().build()
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
