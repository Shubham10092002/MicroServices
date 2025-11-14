package com.example.wallet_service.security;

public class JwtRequestContext {
    private static final ThreadLocal<String> currentToken = new ThreadLocal<>();

    public static void setToken(String token) {
        currentToken.set(token);
    }

    public static String getToken() {
        return currentToken.get();
    }

    public static void clear() {
        currentToken.remove();
    }
}
