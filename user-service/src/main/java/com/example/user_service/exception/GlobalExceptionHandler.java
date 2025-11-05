package com.example.user_service.exception;

import jakarta.persistence.OptimisticLockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String invalidValue = ex.getValue() != null ? ex.getValue().toString() : "null";
        logger.warn("Invalid ID format: {}", invalidValue, ex);

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("error", "Bad Request");
        error.put("message", "Invalid ID format: " + invalidValue + " (must be numeric)");

        return ResponseEntity.badRequest().body(error);
    }



    @ExceptionHandler(LimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleLimitExceeded(LimitExceededException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),                // timestamp
                HttpStatus.BAD_REQUEST.value(),     // status
                ex.getErrorCode(),                  // errorCode
                ex.getMessage(),                    // message
                null                                // fieldErrors (none for this exception)
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }



    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("errorCode", "USER_NOT_FOUND");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<?> handleOptimisticLockException(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "errorCode", "CONFLICT",
                "reason", "Wallet was modified by another transaction. Please retry."
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("errorCode", "VALIDATION_ERROR");
        response.put("message", "Validation failed for one or more fields");
        response.put("fieldErrors", errors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(WalletIdNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleWalletNotFound(WalletIdNotFoundException ex) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.NOT_FOUND.value());
        error.put("errorCode", "WALLET_NOT_FOUND");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        logger.error("Unhandled exception: ", ex);

        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", HttpStatus.BAD_REQUEST.value());
        error.put("errorCode", "GENERIC_ERROR");
        error.put("message", ex.getMessage());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }
}
