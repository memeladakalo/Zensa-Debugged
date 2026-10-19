package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.preferences.UserPreferences
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.ChangePasswordRequestDto
import com.zensa.app.data.remote.dto.UpdateProfileRequestDto
import com.zensa.app.data.remote.dto.UserProfileDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * SettingsRepository
 *
 * Bridges three sources of truth:
 *   - UserPreferences (DataStore): theme, notifications, defaults
 *   - ZensaApiService (Retrofit):    profile update, password change
 *   - AuthRepository:                current user identity
 *
 * The ViewModel only talks to this repository — it never touches
 * DataStore or Retrofit directly.
 */
class SettingsRepository(
    private val preferences: UserPreferences,
    private val api: ZensaApiService
) {

    // ---------------- Local preferences (flows) ----------------

    val theme: Flow<String> = preferences.theme
    val notificationsEnabled: Flow<Boolean> = preferences.notificationsEnabled
    val defaultStudyDuration: Flow<Int> = preferences.defaultStudyDurationMinutes
    val defaultPriority: Flow<String> = preferences.defaultPriority
    val lastSyncAt: Flow<String?> = preferences.lastSyncAt

    // ---------------- Local preferences (writes) ----------------

    suspend fun setTheme(value: String) = preferences.setTheme(value)

    suspend fun setNotificationsEnabled(value: Boolean) =
        preferences.setNotificationsEnabled(value)

    suspend fun setDefaultStudyDuration(minutes: Int) =
        preferences.setDefaultStudyDuration(minutes)

    suspend fun setDefaultPriority(priority: String) =
        preferences.setDefaultPriority(priority)

    suspend fun markSyncedNow() {
        val now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        preferences.setLastSyncAt(now)
    }

    // ---------------- Remote profile operations ----------------

    /** Pull the current user's profile from the server. */
    suspend fun fetchProfile(): ApiResult<UserProfileDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.me()
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error("Could not load profile (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    /** Update the current user's display name. */
    suspend fun updateName(newName: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateProfile(UpdateProfileRequestDto(name = newName))
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.Error("Could not update name (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    /** Change the current user's password. */
    suspend fun changePassword(
        currentPassword: String,
        newPassword: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.changePassword(
                ChangePasswordRequestDto(
                    currentPassword = currentPassword,
                    newPassword = newPassword
                )
            )
            if (response.isSuccessful) {
                ApiResult.Success(Unit)
            } else if (response.code() == 401) {
                ApiResult.Error("Current password is incorrect")
            } else {
                ApiResult.Error("Could not change password (${response.code()})")
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }
}
