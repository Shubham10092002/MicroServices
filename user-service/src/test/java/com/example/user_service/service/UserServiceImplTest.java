package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserSummaryDTO;
import com.example.user_service.exception.InvalidCredentialsException;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletClient walletClient;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    // ------------------- REGISTER USER TESTS -------------------
    @Nested
    @DisplayName("Register User Tests")
    class RegisterUserTests {

        @Test
        @DisplayName("should register user successfully")
        void registerUser_success() {
            UserDTO dto = new UserDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");
            dto.setRole("USER");

            User savedUser = new User();
            savedUser.setId(1L);
            savedUser.setUsername("shiv");
            savedUser.setPassword("encoded");
            savedUser.setRole("USER");

            when(userRepository.findByUsername("shiv")).thenReturn(Optional.empty());
            when(passwordEncoder.encode("StrongPass123")).thenReturn("encoded");
            when(userRepository.save(any(User.class))).thenReturn(savedUser);

            ResponseEntity<?> response = userService.registerUser(dto);

            assertEquals(200, response.getStatusCodeValue());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertEquals("User registered successfully", body.get("message"));
            assertEquals("shiv", body.get("username"));
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("should return 400 if username already exists")
        void registerUser_usernameExists() {
            UserDTO dto = new UserDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");

            when(userRepository.findByUsername("shiv")).thenReturn(Optional.of(new User()));

            ResponseEntity<?> response = userService.registerUser(dto);

            assertEquals(400, response.getStatusCodeValue());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertEquals("Username already exists", body.get("error"));
        }

        @Test
        @DisplayName("should return 500 if exception occurs during registration")
        void registerUser_internalError() {
            UserDTO dto = new UserDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");

            when(userRepository.findByUsername("shiv")).thenThrow(new RuntimeException("DB failure"));

            ResponseEntity<?> response = userService.registerUser(dto);

            assertEquals(500, response.getStatusCodeValue());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertEquals("USER_REGISTRATION_ERROR", body.get("errorCode"));
        }
    }

    // ------------------- LOGIN USER TESTS -------------------
    @Nested
    @DisplayName("Login User Tests")
    class LoginUserTests {

        @Test
        @DisplayName("should login successfully and return JWT token")
        void loginUser_success() {
            LoginRequestDTO dto = new LoginRequestDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");

            User user = new User();
            user.setId(1L);
            user.setUsername("shiv");
            user.setRole("USER");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("shiv")).thenReturn(Optional.of(user));
            when(jwtUtil.generateToken(1L, "shiv", "USER")).thenReturn("mock-jwt");

            ResponseEntity<?> response = userService.loginUser(dto);

            assertEquals(200, response.getStatusCodeValue());
            Map<?, ?> body = (Map<?, ?>) response.getBody();
            assertEquals("mock-jwt", body.get("token"));
            assertEquals("shiv", body.get("username"));
        }

        @Test
        @DisplayName("should throw InvalidCredentialsException for invalid credentials")
        void loginUser_invalidCredentials() {
            LoginRequestDTO dto = new LoginRequestDTO();
            dto.setUsername("shiv");
            dto.setPassword("wrong");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            assertThrows(InvalidCredentialsException.class, () -> userService.loginUser(dto));
        }

        @Test
        @DisplayName("should throw RuntimeException for internal authentication error")
        void loginUser_internalAuthError() {
            LoginRequestDTO dto = new LoginRequestDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new RuntimeException("Auth service down"));

            assertThrows(RuntimeException.class, () -> userService.loginUser(dto));
        }

        @Test
        @DisplayName("should throw UserNotFoundException if user not found after successful authentication")
        void loginUser_userNotFound() {
            LoginRequestDTO dto = new LoginRequestDTO();
            dto.setUsername("shiv");
            dto.setPassword("StrongPass123");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(userRepository.findByUsername("shiv")).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class, () -> userService.loginUser(dto));
        }
    }

    // ------------------- GET USER BY ID TESTS -------------------
    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user summary successfully")
        void getUserById_success() {
            User user = new User();
            user.setId(1L);
            user.setUsername("shiv");

            when(userRepository.findById(1L)).thenReturn(Optional.of(user));

            UserSummaryDTO result = userService.getUserById(1L);

            assertEquals(1L, result.getId());
            assertEquals("shiv", result.getUsername());
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void getUserById_notFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            RuntimeException ex = assertThrows(RuntimeException.class,
                    () -> userService.getUserById(99L));

            assertTrue(ex.getMessage().contains("User not found"));
        }
    }
}
