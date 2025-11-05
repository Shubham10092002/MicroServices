package com.example.wallet_service.exception;

import java.time.LocalDateTime;
import java.util.Map;

public class ApiError {
    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private Map<String, String> fieldErrors;

    public ApiError() {}

    public ApiError(LocalDateTime timestamp, int status, String errorCode, String message, Map<String, String> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    // getters / setters
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(Map<String, String> fieldErrors) { this.fieldErrors = fieldErrors; }
}
