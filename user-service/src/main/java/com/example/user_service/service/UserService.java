package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.*;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
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
    // 🔹 NEW: USER REGISTRATION LOGIC
    // ============================================================
    public ResponseEntity<?> registerUser(User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            logger.warn("Username '{}' already exists", user.getUsername());
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        User savedUser = userRepository.save(user);
        logger.info("✅ User '{}' registered successfully", savedUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "userId", savedUser.getId(),
                "role", savedUser.getRole()
        ));
    }

    // ============================================================
    // 🔹 NEW: USER LOGIN LOGIC
    // ============================================================
    public ResponseEntity<?> loginUser(User user) {
        // Authenticate user credentials
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword())
        );

        User existingUser = userRepository.findByUsername(user.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate JWT token
        //String token = jwtUtil.generateToken(existingUser.getUsername(), existingUser.getRole());
        String token = jwtUtil.generateToken(existingUser.getId(), existingUser.getUsername(), existingUser.getRole());

        logger.info("✅ User '{}' logged in successfully", existingUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "username", existingUser.getUsername(),
                "role", existingUser.getRole()
        ));
    }

    // ============================================================
    // 🔹 EXISTING BUSINESS METHODS
    // ============================================================

    public List<UserResponseDTO> getUsersWithWalletBalanceGreaterThan(BigDecimal threshold) {
        logger.info("Fetching users with wallet balance greater than {}", threshold);

        List<Wallet> wallets = walletClient.getWalletsWithBalanceGreaterThan(threshold);
        List<UserResponseDTO> result = new ArrayList<>();

        for (Wallet wallet : wallets) {
            userRepository.findById(wallet.getUserId()).ifPresent(user ->
                    result.add(new UserResponseDTO(user, wallet)));
        }

        return result;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public Object createUser(@Valid UserDTO userDTO, BindingResult result) {
        logger.info("Received request to create user: {}", userDTO.getUsername());

        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
            return errors;
        }

        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            logger.warn("Username '{}' already exists", userDTO.getUsername());
            return "Username already exists";
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String jwtToken = null;
        if (authentication != null && authentication.getCredentials() instanceof String token) {
            jwtToken = token;
        }

        User user = new User(userDTO.getUsername(),
                passwordEncoder.encode(userDTO.getPassword()),
                userDTO.getRole());
        User savedUser = userRepository.save(user);
        logger.info("User created with ID {}", savedUser.getId());

        Wallet wallet = walletClient.createDefaultWalletForUser(savedUser.getId(),
                savedUser.getUsername(), jwtToken);

        if (wallet == null) {
            logger.error("Wallet creation failed for user {}", savedUser.getId());
            return "User created, but wallet creation failed";
        }

        logger.info("Wallet created successfully for user {}", savedUser.getId());
        return new UserResponseDTO(savedUser, wallet);
    }

    public UserSummaryDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
    }

    public List<UserSummaryDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
    }
}
