package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final WalletClient walletClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserService(UserRepository userRepository,
                       WalletClient walletClient,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.walletClient = walletClient;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
    }

    // ============================================================
    // 🔹 Register New User Using UserDTO
    // ============================================================
    public ResponseEntity<?> registerUser(UserDTO userDTO) {
        logger.info("Register request for username: {}", userDTO.getUsername());

        // Check if username already exists
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        // Create and save new user
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole() != null ? userDTO.getRole() : "USER");

        User savedUser = userRepository.save(user);
        logger.info(" User '{}' registered successfully with ID {}", savedUser.getUsername(), savedUser.getId());

        // Create default wallet (JWT not required at registration)
//        try {
//            walletClient.createDefaultWalletForUser(savedUser.getId(), savedUser.getUsername(), null);
//        } catch (Exception e) {
//            logger.error("⚠️ Wallet creation failed for user {}", savedUser.getId());
//        }

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", savedUser.getId(),
                "username", savedUser.getUsername(),
                "role", savedUser.getRole()
        ));
    }

    // ============================================================
    // 🔹 Login Using LoginRequestDTO
    // ============================================================
    public ResponseEntity<?> loginUser(LoginRequestDTO loginDTO) {
        logger.info("Login attempt by '{}'", loginDTO.getUsername());

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
        );

        User existingUser = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT with userId + username + role
        String token = jwtUtil.generateToken(
                existingUser.getId(),
                existingUser.getUsername(),
                existingUser.getRole()
        );

        logger.info("✅ User '{}' logged in successfully", existingUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", existingUser.getId(),
                "username", existingUser.getUsername(),
                "role", existingUser.getRole()
        ));
    }


    public UserSummaryDTO getUserById(Long id) {
        logger.info("Fetching user with ID {}", id);

        return userRepository.findById(id)
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
    }
}
