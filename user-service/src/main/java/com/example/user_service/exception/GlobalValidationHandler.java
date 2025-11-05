package com.example.user_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalValidationHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalValidationHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField, FieldError::getDefaultMessage, (a, b) -> a));

        ApiError error = new ApiError(LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Request validation failed",
                fieldErrors);

        logger.warn("Validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // reuse your other exception handlers (UserNotFoundException, WalletIdNotFoundException) or keep them in your existing GlobalExceptionHandler.
}
