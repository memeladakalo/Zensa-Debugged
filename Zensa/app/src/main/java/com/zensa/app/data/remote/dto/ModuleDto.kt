package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ModuleDto(
    val moduleId: String,
    val userId: String,
    val moduleCode: String,
    val name: String,
    val description: String,
    val colour: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class ModuleRequestDto(
    val moduleCode: String,
    val name: String,
    val description: String,
    val colour: String
)
