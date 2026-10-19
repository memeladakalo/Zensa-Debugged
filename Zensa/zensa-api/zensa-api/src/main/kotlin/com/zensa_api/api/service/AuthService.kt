package com.zensa.api.service

import com.zensa.api.config.JwtService
import com.zensa.api.dto.LoginRequest
import com.zensa.api.dto.LoginResponse
import com.zensa.api.dto.RegisterRequest
import com.zensa.api.dto.RegisterResponse
import com.zensa.api.entity.UserEntity
import com.zensa.api.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {

    fun register(request: RegisterRequest): RegisterResponse {
        val email = request.email.trim().lowercase()

        if (userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("An account with this email already exists")
        }

        val hashedPassword = passwordEncoder.encode(request.password)

        val user = UserEntity(
            name = request.name.trim(),
            email = email,
            passwordHash = hashedPassword
        )

        val saved = userRepository.save(user)

        return RegisterResponse(
            userId = saved.userId!!.toString(),
            name = saved.name,
            email = saved.email
        )
    }

    /**
     * Real login:
     *  1. Find user by email (case-insensitive).
     *  2. Compare submitted password to the stored BCrypt hash.
     *  3. Issue a signed JWT.
     *
     * If either step fails, throw IllegalArgumentException so the
     * GlobalExceptionHandler returns 401 to the client.
     */
    fun login(request: LoginRequest): LoginResponse {
        val email = request.email.trim().lowercase()
        val user = userRepository.findByEmail(email)
            ?: throw IllegalArgumentException("Incorrect email or password")

        val passwordMatches = passwordEncoder.matches(request.password, user.passwordHash)
        if (!passwordMatches) {
            throw IllegalArgumentException("Incorrect email or password")
        }

        val token = jwtService.generateToken(user.userId!!.toString(), user.email)

        return LoginResponse(
            token = token,
            userId = user.userId.toString(),
            name = user.name,
            email = user.email
        )
    }
}
