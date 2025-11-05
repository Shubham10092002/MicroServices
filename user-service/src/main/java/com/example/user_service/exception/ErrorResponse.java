package com.example.user_service.exception;

import java.time.LocalDateTime;
import java.util.List;

public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String errorCode;
    private String message;
    private List<FieldErrorDetail> fieldErrors;

    public ErrorResponse() {}

    public ErrorResponse(LocalDateTime timestamp, int status, String errorCode, String message, List<FieldErrorDetail> fieldErrors) {
        this.timestamp = timestamp;
        this.status = status;
        this.errorCode = errorCode;
        this.message = message;
        this.fieldErrors = fieldErrors;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<FieldErrorDetail> getFieldErrors() { return fieldErrors; }
    public void setFieldErrors(List<FieldErrorDetail> fieldErrors) { this.fieldErrors = fieldErrors; }
}
