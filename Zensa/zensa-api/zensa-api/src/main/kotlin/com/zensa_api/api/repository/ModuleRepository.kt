package com.zensa.api.repository

import com.zensa.api.entity.ModuleEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ModuleRepository : JpaRepository<ModuleEntity, UUID> {

    /** All modules belonging to a specific user. */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<ModuleEntity>

    /** Prevents duplicate module codes for the same user. */
    fun existsByUserIdAndModuleCodeIgnoreCase(userId: UUID, moduleCode: String): Boolean

    /** Locate a specific user's module. */
    fun findByModuleIdAndUserId(moduleId: UUID, userId: UUID): ModuleEntity?
}
