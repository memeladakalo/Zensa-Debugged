package com.zensa.api.config

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

/**
 * JwtService — creates and validates JSON Web Tokens.
 *
 * A JWT is a signed, base64-encoded string. The server signs it with
 * a secret key. When the client sends it back in the Authorization
 * header, the server can verify the signature — which proves the
 * token was issued by us and has not been tampered with.
 *
 * This is NOT encryption. Anyone can read the token's payload.
 * Do not put sensitive data (passwords, secrets) in it.
 */
@Service
class JwtService(
    @Value("\${zensa.jwt.secret}") private val secret: String,
    @Value("\${zensa.jwt.expiration-ms}") private val expirationMs: Long
) {
    private val key: SecretKey = Keys.hmacShaKeyFor(secret.toByteArray())

    /** Creates a token for [userId] with subject [email]. */
    fun generateToken(userId: String, email: String): String {
        val now = Date()
        return Jwts.builder()
            .subject(email)
            .claim("userId", userId)
            .issuedAt(now)
            .expiration(Date(now.time + expirationMs))
            .signWith(key)
            .compact()
    }

    /** Returns the email (subject) if the token is valid, else null. */
    fun extractEmail(token: String): String? = try {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload.subject
    } catch (e: Exception) {
        null
    }

    fun extractUserId(token: String): String? = try {
        Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).payload["userId", String::class.java)
    } catch (e: Exception) {
        null
    }

    fun isValid(token: String): Boolean = extractEmail(token) != null
}
