package com.zensa.api.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Reads the "Authorization: Bearer <token>" header on every request.
 * If the token is valid, it sets the authenticated user in the
 * SecurityContext. Otherwise the request is unauthenticated and
 * Spring Security rejects it.
 */
@Component
class JwtAuthFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val header = request.getHeader("Authorization")
        if (header != null && header.startsWith("Bearer ")) {
            val token = header.removePrefix("Bearer ").trim()
            if (jwtService.isValid(token)) {
                val email = jwtService.extractEmail(token)
                val userId = jwtService.extractUserId(token)
                val auth = UsernamePasswordAuthenticationToken(
                    email, null, emptyList()
                )
                auth.details = userId
                SecurityContextHolder.getContext().authentication = auth
            }
        }
        filterChain.doFilter(request, response)
    }
}
