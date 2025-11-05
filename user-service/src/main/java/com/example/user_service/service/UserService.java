package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserResponseDTO;
import com.example.user_service.dto.UserSummaryDTO;
import com.example.user_service.model.User;
import com.example.user_service.dto.Wallet;
import com.example.user_service.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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


    public UserService(UserRepository userRepository, WalletClient walletClient) {
        this.userRepository = userRepository;
        this.walletClient = walletClient;
    }

    public List<UserResponseDTO> getUsersWithWalletBalanceGreaterThan(BigDecimal threshold) {
        logger.info("Fetching users with wallet balance greater than {}", threshold);

        List<Wallet> wallets = walletClient.getWalletsWithBalanceGreaterThan(threshold);
        List<UserResponseDTO> result = new ArrayList<>();

        for (Wallet wallet : wallets) {
            userRepository.findById(wallet.getUserId()).ifPresent(user -> {
                result.add(new UserResponseDTO(user, wallet));
            });
        }

        return result;
    }


    public Object createUser(@Valid UserDTO userDTO, BindingResult result) {
        logger.info("Received request to create user: {}", userDTO.getUsername());

        // Validation handling
        if (result.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            result.getFieldErrors().forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
            return errors;
        }

        // Duplicate username check
        if (userRepository.findByUsername(userDTO.getUsername()).isPresent()) {
            logger.warn("Username '{}' already exists", userDTO.getUsername());
            return "Username already exists";
        }

        // Save user
        User user = new User(userDTO.getUsername(), userDTO.getPassword(), userDTO.getRole());
        User savedUser = userRepository.save(user);
        logger.info("User created with ID {}", savedUser.getId());

        // Create default wallet via wallet-service
        //Wallet wallet = walletClient.createDefaultWalletForUser(savedUser.getId());
        Wallet wallet = walletClient.createDefaultWalletForUser(savedUser.getId(), savedUser.getUsername());


        if (wallet == null) {
            logger.error("Wallet creation failed for user {}", savedUser.getId());
            return "User created, but wallet creation failed";
        }

        logger.info("Wallet created successfully for user {}", savedUser.getId());
        return new UserResponseDTO(savedUser, wallet);
    }

//    public User getUserById(Long id) {
//        return userRepository.findById(id)
//                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
//    }


    public UserSummaryDTO getUserById(Long id) {
        return userRepository.findById(id)
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .orElseThrow(() -> new RuntimeException("User not found with ID " + id));
    }


//
//    public java.util.List<User> getAllUsers() {
//        return userRepository.findAll();
//    }

    public List<UserSummaryDTO> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(user -> new UserSummaryDTO(user.getId(), user.getUsername()))
                .collect(Collectors.toList());
    }

}
