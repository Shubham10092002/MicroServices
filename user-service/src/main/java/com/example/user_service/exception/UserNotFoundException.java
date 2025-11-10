package com.example.user_service.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User not found with username: " + username);
    }
}
