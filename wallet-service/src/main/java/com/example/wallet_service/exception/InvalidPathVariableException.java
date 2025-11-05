package com.example.wallet_service.exception;

public class InvalidPathVariableException extends RuntimeException {


    public InvalidPathVariableException(String message) {
        super(message);
    }

    public InvalidPathVariableException(Throwable cause) {
        super(cause);
    }

    public InvalidPathVariableException(String message, Throwable cause) {
        super(message, cause);
    }
}
