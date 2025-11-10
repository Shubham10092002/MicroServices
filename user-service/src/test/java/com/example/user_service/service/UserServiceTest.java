package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.dto.UserSummaryDTO;
import com.example.user_service.exception.WalletServiceException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

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
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("shiv");
        testUser.setPassword("encodedPass");
        testUser.setRole("USER");
    }

    // =====================================================
    // ✅ TEST: Register User - SUCCESS CASE
    // =====================================================
    @Test
    @DisplayName("Should register new user successfully")
    void testRegisterUser_Success() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("shiv");
        userDTO.setPassword("password");
        userDTO.setRole("USER");

        when(userRepository.findByUsername("shiv")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        ResponseEntity<?> response = userService.registerUser(userDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("User registered successfully", body.get("message"));
        verify(userRepository).save(any(User.class));
    }

    // =====================================================
    // ⚠️ TEST: Register User - USERNAME ALREADY EXISTS
    // =====================================================
    @Test
    @DisplayName("Should fail registration when username already exists")
    void testRegisterUser_UsernameAlreadyExists() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("shiv");
        userDTO.setPassword("password");

        when(userRepository.findByUsername("shiv")).thenReturn(Optional.of(testUser));

        ResponseEntity<?> response = userService.registerUser(userDTO);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("Username already exists", body.get("error"));
        verify(userRepository, never()).save(any(User.class));
    }

    // =====================================================
    // ⚠️ TEST: Register User - INTERNAL SERVER ERROR
    // =====================================================
    @Test
    @DisplayName("Should handle unexpected exception during registration")
    void testRegisterUser_InternalServerError() {
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername("shiv");
        userDTO.setPassword("password");

        when(userRepository.findByUsername("shiv")).thenThrow(new RuntimeException("DB connection failed"));

        ResponseEntity<?> response = userService.registerUser(userDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("USER_REGISTRATION_ERROR", body.get("errorCode"));
    }

    // =====================================================
    // ✅ TEST: Login User - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Should login user successfully and return JWT token")
    void testLoginUser_Success() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setUsername("shiv");
        loginDTO.setPassword("password");

        when(userRepository.findByUsername("shiv")).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(1L, "shiv", "USER")).thenReturn("mock-jwt-token");

        ResponseEntity<?> response = userService.loginUser(loginDTO);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("mock-jwt-token", body.get("token"));
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    // =====================================================
    // ⚠️ TEST: Login User - INVALID CREDENTIALS
    // =====================================================
    @Test
    @DisplayName("Should return unauthorized for invalid credentials")
    void testLoginUser_InvalidCredentials() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setUsername("shiv");
        loginDTO.setPassword("wrongpass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        ResponseEntity<?> response = userService.loginUser(loginDTO);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID_CREDENTIALS", body.get("errorCode"));
    }

    // =====================================================
    // ⚠️ TEST: Login User - AUTHENTICATION FAILURE
    // =====================================================
    @Test
    @DisplayName("Should handle internal error during authentication")
    void testLoginUser_AuthenticationError() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setUsername("shiv");
        loginDTO.setPassword("password");

        doThrow(new RuntimeException("Authentication system down"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        ResponseEntity<?> response = userService.loginUser(loginDTO);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("AUTHENTICATION_ERROR", body.get("errorCode"));
    }

    // =====================================================
    // ⚠️ TEST: Login User - USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Should return 404 when user not found during login")
    void testLoginUser_UserNotFound() {
        LoginRequestDTO loginDTO = new LoginRequestDTO();
        loginDTO.setUsername("unknown");
        loginDTO.setPassword("password");

        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        ResponseEntity<?> response = userService.loginUser(loginDTO);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("USER_NOT_FOUND", body.get("errorCode"));
    }

    // =====================================================
    // ✅ TEST: Get User By ID - SUCCESS
    // =====================================================
    @Test
    @DisplayName("Should fetch user summary by ID successfully")
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        var result = userService.getUserById(1L);

        assertEquals("shiv", result.getUsername());
        verify(userRepository).findById(1L);
    }

    // =====================================================
    // ⚠️ TEST: Get User By ID - USER NOT FOUND
    // =====================================================
    @Test
    @DisplayName("Should throw exception when user not found by ID")
    void testGetUserById_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.getUserById(1L));
        assertEquals("User not found with ID 1", ex.getMessage());
    }
}
