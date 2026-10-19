package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class NoteDto(
    val noteId: String,
    val userId: String,
    val moduleId: String? = null,
    val title: String,
    val content: String = "",
    val attachmentUrl: String? = null,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class NoteRequestDto(
    val moduleId: String? = null,
    val title: String,
    val content: String = "",
    val attachmentUrl: String? = null
)
