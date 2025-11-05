package com.example.user_service.exception;

public class LimitExceededException extends RuntimeException {
    private final String errorCode;

    public LimitExceededException(String message) {
        super(message);
        this.errorCode = "LIMIT_EXCEEDED";
    }

    public LimitExceededException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
