package com.example.user_service.security;

import com.example.user_service.exception.JwtValidationException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.util.Date;


@Component
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private final Key secretKey;
    private final long expirationTime;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expirationTime
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.expirationTime = expirationTime;
    }

    // ====================================================
    // 🔹 Generate Token (if needed)
    // ====================================================

    public String generateToken(Long userId, String username, String role) {
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    // ====================================================
    // 🔹 Extract Claims
    // ====================================================

    private Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException ex) {
            logger.warn("JWT expired: {}", ex.getMessage());
            throw new JwtValidationException("TOKEN_EXPIRED", "JWT token has expired");
        } catch (UnsupportedJwtException ex) {
            logger.warn("Unsupported JWT: {}", ex.getMessage());
            throw new JwtValidationException("TOKEN_UNSUPPORTED", "JWT token format not supported");
        } catch (MalformedJwtException ex) {
            logger.warn("Malformed JWT: {}", ex.getMessage());
            throw new JwtValidationException("TOKEN_MALFORMED", "JWT token structure is invalid");
        } catch (SignatureException ex) {
            logger.warn("Invalid JWT signature: {}", ex.getMessage());
            throw new JwtValidationException("TOKEN_SIGNATURE_INVALID", "JWT signature validation failed");
        } catch (IllegalArgumentException ex) {
            logger.warn("Illegal JWT token: {}", ex.getMessage());
            throw new JwtValidationException("TOKEN_INVALID", "JWT token is empty or null");
        }
    }

    // ====================================================
    // 🔹 Extract individual claims safely
    // ====================================================
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    public Long extractUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    // ====================================================
    // 🔹 Validate token with detailed error reporting
    // ====================================================
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtValidationException e) {
            logger.error("Token validation failed: {} - {}", e.getErrorCode(), e.getMessage());
            throw e; // rethrow for global handler
        }
    }
}
