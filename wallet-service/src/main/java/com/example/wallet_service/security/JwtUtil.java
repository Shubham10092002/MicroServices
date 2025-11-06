package com.example.wallet_service.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String SECRET = "your-256-bit-secret-your-256-bit-secret"; // same as user-service
    private static final long EXPIRATION_TIME = 86400000; // 1 day

    private final Key key = Keys.hmacShaKeyFor(SECRET.getBytes());

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return (String) parseClaims(token).get("role");
    }

    public Long extractUserId(String token) {
        Object userId = parseClaims(token).get("userId");
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
