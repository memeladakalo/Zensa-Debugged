package com.zensa.api.service

import com.zensa.api.dto.ModuleRequest
import com.zensa.api.dto.ModuleResponse
import com.zensa.api.entity.ModuleEntity
import com.zensa.api.repository.ModuleRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ModuleService(
    private val moduleRepository: ModuleRepository
) {

    private val iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    fun listForUser(userId: UUID): List<ModuleResponse> =
        moduleRepository.findAllByUserIdOrderByCreatedAtDesc(userId).map { it.toResponse() }

    fun create(userId: UUID, request: ModuleRequest): ModuleResponse {
        val code = request.moduleCode.trim().uppercase()

        if (moduleRepository.existsByUserIdAndModuleCodeIgnoreCase(userId, code)) {
            throw IllegalArgumentException("You already have a module with this code")
        }

        val entity = ModuleEntity(
            userId = userId,
            moduleCode = code,
            name = request.name.trim(),
            description = request.description.trim(),
            colour = request.colour.ifBlank { "#6A4BAF" }
        )

        return moduleRepository.save(entity).toResponse()
    }

    fun update(userId: UUID, moduleId: UUID, request: ModuleRequest): ModuleResponse {
        val entity = moduleRepository.findByModuleIdAndUserId(moduleId, userId)
            ?: throw IllegalArgumentException("Module not found")

        entity.moduleCode = request.moduleCode.trim().uppercase()
        entity.name = request.name.trim()
        entity.description = request.description.trim()
        entity.colour = request.colour.ifBlank { "#6A4BAF" }
        entity.updatedAt = LocalDateTime.now()

        return moduleRepository.save(entity).toResponse()
    }

    fun delete(userId: UUID, moduleId: UUID) {
        val entity = moduleRepository.findByModuleIdAndUserId(moduleId, userId)
            ?: throw IllegalArgumentException("Module not found")
        moduleRepository.delete(entity)
    }

    private fun ModuleEntity.toResponse() = ModuleResponse(
        moduleId = moduleId.toString(),
        userId = userId.toString(),
        moduleCode = moduleCode,
        name = name,
        description = description,
        colour = colour,
        createdAt = createdAt.format(iso),
        updatedAt = updatedAt.format(iso)
    )
}
