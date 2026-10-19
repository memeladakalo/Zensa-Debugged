package com.zensa.api.repository

import com.zensa.api.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for UserEntity.
 *
 * JpaRepository already provides save, findById, findAll, delete,
 * count, and more. The methods below are Zensa-specific queries
 * that Spring generates from their names.
 */
interface UserRepository : JpaRepository<UserEntity, UUID> {

    /** Used by register (duplicate check) and login (find by email). */
    fun findByEmail(email: String): UserEntity?

    /** Used by register to check whether an email already exists. */
    fun existsByEmail(email: String): Boolean
}
