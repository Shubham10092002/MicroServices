package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.*;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.exception.InvalidCredentialsException;

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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserServiceImpl implements UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserRepository userRepository;
    private final WalletClient walletClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    public UserServiceImpl(UserRepository userRepository,
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

    @Override
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
            logger.info("User '{}' registered successfully (ID: {})", savedUser.getUsername(), savedUser.getId());

            return ResponseEntity.ok(Map.of(
                    "message", "User registered successfully",
                    "userId", savedUser.getId(),
                    "username", savedUser.getUsername(),
                    "role", savedUser.getRole()
            ));

        } catch (Exception e) {
            logger.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                    "errorCode", "USER_REGISTRATION_ERROR",
                    "message", e.getMessage()
            ));
        }
    }


    public User toggleBlacklistStatus(Long id, boolean status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID " + id));
        user.setBlacklisted(status);
        userRepository.save(user);

        logger.info("User {} blacklisted status set to {}", user.getUsername(), status);
        return user;
    }



    @Override
    public ResponseEntity<?> loginUser(LoginRequestDTO loginDTO) {
        logger.info("Login attempt by '{}'", loginDTO.getUsername());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginDTO.getUsername(), loginDTO.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new InvalidCredentialsException("Incorrect username or password");
        } catch (Exception ex) {
            throw new RuntimeException("Login failed due to internal error", ex);
        }

        User existingUser = userRepository.findByUsername(loginDTO.getUsername())
                .orElseThrow(() -> new UserNotFoundException("User not found: " + loginDTO.getUsername()));

        //  Block login if blacklisted
        if (existingUser.isBlacklisted()) {
            logger.warn("Blocked login attempt by blacklisted user '{}'", existingUser.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "errorCode", "USER_BLACKLISTED",
                    "message", "Your account has been blacklisted. Please contact support."
            ));
        }

        String token = jwtUtil.generateToken(
                existingUser.getId(),
                existingUser.getUsername(),
                existingUser.getRole()
        );

        logger.info("User '{}' logged in successfully", existingUser.getUsername());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", existingUser.getId(),
                "username", existingUser.getUsername(),
                "role", existingUser.getRole()
        ));
    }


    public UserDetailsDTO getUserDetails(Long id)  {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));

        UserDetailsDTO dto = new UserDetailsDTO(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                user.isBlacklisted()
        );

        return dto;
    }





    @Override
    public UserSummaryDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
    }

    public User findById(Long id){
        return  userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID " + id));
    }

}
