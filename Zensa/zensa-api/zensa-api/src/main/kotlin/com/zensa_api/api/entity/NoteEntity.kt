package com.zensa.api.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "notes")
class NoteEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "note_id", updatable = false, nullable = false)
    var noteId: UUID? = null,

    @Column(name = "user_id", nullable = false)
    var userId: UUID? = null,

    @Column(name = "module_id")
    var moduleId: UUID? = null,

    @Column(nullable = false, length = 200)
    var title: String = "",

    @Lob
    @Column(columnDefinition = "TEXT")
    var content: String = "",

    @Column(name = "attachment_url", length = 500)
    var attachmentUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
