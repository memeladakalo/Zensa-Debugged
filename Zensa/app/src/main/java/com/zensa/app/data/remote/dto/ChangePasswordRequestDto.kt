package com.zensa.app.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Request body for PUT /api/users/me/password.
 */
@Serializable
data class ChangePasswordRequestDto(
    val currentPassword: String,
    val newPassword: String
)
