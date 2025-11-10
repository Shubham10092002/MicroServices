package com.example.user_service.exception;

public class InvalidCredentialsException extends RuntimeException {
    private final String errorCode;

    public InvalidCredentialsException(String message) {
        super(message);
        this.errorCode = "INVALID_CREDENTIALS";
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "INVALID_CREDENTIALS";
    }

    public String getErrorCode() {
        return errorCode;
    }
}
