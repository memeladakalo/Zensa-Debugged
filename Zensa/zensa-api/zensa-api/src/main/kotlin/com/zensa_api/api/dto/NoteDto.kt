package com.zensa.api.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class NoteRequest(
    val moduleId: String? = null,

    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200, message = "Title is too long")
    val title: String = "",

    val content: String = "",

    val attachmentUrl: String? = null
)

data class NoteResponse(
    val noteId: String,
    val userId: String,
    val moduleId: String?,
    val title: String,
    val content: String,
    val attachmentUrl: String?,
    val createdAt: String,
    val updatedAt: String
)
