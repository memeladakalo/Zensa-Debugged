package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

/**
 * Server-side Module entity. Mirrors the Android Room ModuleEntity
 * so the DTOs map cleanly in both directions.
 */
@Entity
@Table(name = "modules")
class ModuleEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "module_id", updatable = false, nullable = false)
    var moduleId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "module_code", nullable = false, length = 20)
    var moduleCode: String = "",

    @Column(nullable = false, length = 100)
    var name: String = "",

    @Column(length = 500)
    var description: String = "",

    @Column(length = 20)
    var colour: String = "#6A4BAF",

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
