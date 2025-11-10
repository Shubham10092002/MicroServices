package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.*;
import com.example.user_service.exception.WalletServiceException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
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

    //  Register User
    public ResponseEntity<?> registerUser(UserDTO userDTO) {
        try {
            logger.info("Register request for username: {}", userDTO.getUsername());

            if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }

            User user = new User();
            user.setUsername(userDTO.getUsername());
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
            user.setRole(userDTO.getRole() != null ? userDTO.getRole() : "USER");

            User savedUser = userRepository.save(user);
            logger.info(" User '{}' registered successfully (ID: {})", savedUser.getUsername(), savedUser.getId());

            // Try creating a wallet for the user (optional)
//            try {
//                walletClient.createDefaultWalletForUser(savedUser.getId(), savedUser.getUsername(), null);
//            } catch (WalletServiceException ex) {
//                logger.error(" Wallet creation failed for user {}: {}", savedUser.getId(), ex.getMessage());
//                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT).body(Map.of(
//                        "message", "User registered, but wallet creation failed",
//                        "userId", savedUser.getId(),
//                        "errorCode", ex.getErrorCode(),
//                        "error", ex.getMessage()
//                ));
//            }

            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "userId", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "role", savedUser.getRole()
            ));

        } catch (Exception e) {
            logger.error(" Registration failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "errorCode", "USER_REGISTRATION_ERROR",
                    "message", e.getMessage()
            ));
        }
    }

    //  Login User
    public ResponseEntity<?> loginUser(LoginRequestDTO loginDTO) {
        logger.info("Login attempt by '{}'", loginDTO.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
        } catch (BadCredentialsException ex) {
            logger.warn(" Invalid login credentials for user '{}'", loginDTO.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "errorCode", "INVALID_CREDENTIALS",
                    "message", "Incorrect username or password"
            ));
        } catch (Exception ex) {
            logger.error(" Authentication failed: {}", ex.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "errorCode", "AUTHENTICATION_ERROR",
                    "message", "Login failed due to internal error"
            ));
        }

        try {
            User existingUser = userRepository.findByUsername(loginDTO.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(
                    existingUser.getId(),
                    existingUser.getUsername(),
                    existingUser.getRole()
            );

            logger.info(" User '{}' logged in successfully", existingUser.getUsername());

            return ResponseEntity.ok(Map.of(
                    "token", token,
                    "userId", existingUser.getId(),
                    "username", existingUser.getUsername(),
                    "role", existingUser.getRole()
            ));
        } catch (RuntimeException ex) {
            logger.error(" Login failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "errorCode", "USER_NOT_FOUND",
                    "message", ex.getMessage()
            ));
        }
    }

    //  Get user by ID (safe fetch)
    public UserSummaryDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
    }
}
