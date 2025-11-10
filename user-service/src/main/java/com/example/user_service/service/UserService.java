package com.example.user_service.service;

import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserSummaryDTO;
import org.springframework.http.ResponseEntity;

public interface UserService {
    ResponseEntity<?> registerUser(UserDTO userDTO);
    ResponseEntity<?> loginUser(LoginRequestDTO loginRequestDTO);
    UserSummaryDTO getUserById(Long id);
}
