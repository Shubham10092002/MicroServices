package com.example.wallet_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("User not found with ID: " + userId);
    }
}
