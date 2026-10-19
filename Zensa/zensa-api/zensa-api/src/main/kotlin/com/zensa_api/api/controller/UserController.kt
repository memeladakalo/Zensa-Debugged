package com.zensa.api.controller

import com.zensa.api.dto.ChangePasswordRequest
import com.zensa.api.dto.UpdateProfileRequest
import com.zensa.api.repository.UserRepository
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) {

    // ---------------- Profile ----------------

    @GetMapping("/me")
    fun me(): ResponseEntity<Map<String, Any>> {
        val email = currentEmail()
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "User not found"))

        return ResponseEntity.ok(
            mapOf(
                "userId" to user.userId.toString(),
                "name" to user.name,
                "email" to user.email
            )
        )
    }

    @PutMapping("/me")
    fun updateProfile(
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<Map<String, Any>> {
        val email = currentEmail()
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "User not found"))

        user.name = request.name.trim()
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        return ResponseEntity.ok(
            mapOf(
                "message" to "Profile updated",
                "name" to user.name
            )
        )
    }

    // ---------------- Password ----------------

    /**
     * PUT /api/users/me/password
     *
     * 1. Confirms the current password matches the stored BCrypt hash.
     * 2. Re-hashes the new password with BCrypt.
     * 3. Saves.
     *
     * If the current password is wrong, returns 401. The Android app
     * shows "Current password is incorrect".
     */
    @PutMapping("/me/password")
    fun changePassword(
        @Valid @RequestBody request: ChangePasswordRequest
    ): ResponseEntity<Map<String, Any>> {
        val email = currentEmail()
            ?: return ResponseEntity.status(401).body(mapOf("message" to "Unauthorized"))

        val user = userRepository.findByEmail(email)
            ?: return ResponseEntity.status(404).body(mapOf("message" to "User not found"))

        val matches = passwordEncoder.matches(request.currentPassword, user.passwordHash)
        if (!matches) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                mapOf("message" to "Current password is incorrect")
            )
        }

        user.passwordHash = passwordEncoder.encode(request.newPassword)
        user.updatedAt = LocalDateTime.now()
        userRepository.save(user)

        return ResponseEntity.ok(mapOf("message" to "Password updated"))
    }

    private fun currentEmail(): String? =
        SecurityContextHolder.getContext().authentication?.name
}
