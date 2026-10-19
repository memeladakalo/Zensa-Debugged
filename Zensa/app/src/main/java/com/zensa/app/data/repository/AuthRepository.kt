package com.zensa.app.data.repository

import com.zensa.app.core.network.ApiResult
import com.zensa.app.data.preferences.TokenStorage
import com.zensa.app.data.remote.ZensaApiService
import com.zensa.app.data.remote.dto.ApiErrorDto
import com.zensa.app.data.remote.dto.LoginRequestDto
import com.zensa.app.data.remote.dto.LoginResponseDto
import com.zensa.app.data.remote.dto.RegisterRequestDto
import com.zensa.app.data.remote.dto.RegisterResponseDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.IOException

class AuthRepository(
    private val api: ZensaApiService,
    private val tokenStorage: TokenStorage
) {

    suspend fun register(
        name: String,
        email: String,
        password: String
    ): ApiResult<RegisterResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.register(RegisterRequestDto(name, email, password))
            if (response.isSuccessful && response.body() != null) {
                ApiResult.Success(response.body()!!)
            } else {
                ApiResult.Error(extractError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    suspend fun login(
        email: String,
        password: String
    ): ApiResult<LoginResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = api.login(LoginRequestDto(email, password))
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                // Persist token and identity
                tokenStorage.saveToken(body.token)
                tokenStorage.saveUser(body.userId, body.name, body.email)
                ApiResult.Success(body)
            } else {
                ApiResult.Error(extractError(response.code(), response.errorBody()?.string()))
            }
        } catch (e: IOException) {
            ApiResult.Error("Cannot reach the server. Check your internet connection.")
        } catch (e: Exception) {
            ApiResult.Error("Unexpected error: ${e.message}")
        }
    }

    fun logout() {
        tokenStorage.clear()
    }

    fun isLoggedIn(): Boolean = tokenStorage.hasToken()

    /**
     * The backend returns {"status": N, "message": "..."} on errors.
     * We try to parse that; otherwise we fall back to a generic message.
     */
    private fun extractError(code: Int, raw: String?): String {
        if (!raw.isNullOrBlank()) {
            try {
                val parsed = Json { ignoreUnknownKeys = true }
                    .decodeFromString(ApiErrorDto.serializer(), raw)
                if (parsed.message.isNotBlank()) return parsed.message
            } catch (_: Exception) { }
        }
        return when (code) {
            400 -> "Please check your details"
            401 -> "Incorrect email or password"
            409 -> "An account with this email already exists"
            500 -> "Server error. Please try again later."
            else -> "Unexpected error (code $code)"
        }
    }
}
