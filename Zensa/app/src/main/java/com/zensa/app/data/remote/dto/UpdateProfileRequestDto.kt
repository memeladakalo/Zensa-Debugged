package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request body for PUT /api/users/me — updates the display name.
 */
@Serializable
data class UpdateProfileRequestDto(
    val name: String
)
