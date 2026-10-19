package com.zensa.api.controller

import com.zensa.api.dto.SyncRequestDto
import com.zensa.api.dto.SyncResponseDto
import com.zensa.api.service.SyncService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * POST /api/sync
 *
 * Accepts a batch of queued offline operations from the Android
 * app and applies them one by one. Returns a per-operation result
 * so the client can:
 *   - store the server-assigned ids for successful CREATEs
 *   - drop the queue row for successful ops
 *   - increment retryCount for failures
 *
 * The endpoint is idempotent for CREATE at the client's discretion:
 * the client removes a queue row as soon as the server confirms,
 * so a re-send of the same queueId will not occur unless the
 * response never reached the device.
 */
@RestController
@RequestMapping("/api/sync")
class SyncController(
    private val syncService: SyncService
) {

    @PostMapping
    fun sync(
        @Valid @RequestBody request: SyncRequestDto
    ): ResponseEntity<SyncResponseDto> {
        val userId = currentUserId()
            ?: return ResponseEntity.status(401).build()
        return ResponseEntity.ok(syncService.applyBatch(userId, request))
    }

    private fun currentUserId(): UUID? {
        val details = SecurityContextHolder.getContext().authentication?.details
        return try {
            if (details is String) UUID.fromString(details) else null
        } catch (_: Exception) { null }
    }
}
