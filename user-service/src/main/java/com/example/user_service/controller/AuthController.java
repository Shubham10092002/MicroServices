package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService; // interface!

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        return userService.loginUser(loginDTO); // ✅ no wrapping
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO) {
        return userService.registerUser(userDTO); // ✅ no wrapping
    }

}
