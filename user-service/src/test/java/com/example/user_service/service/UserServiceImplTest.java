package com.example.user_service.service;

import com.example.user_service.client.WalletClient;
import com.example.user_service.dto.*;
import com.example.user_service.exception.InvalidCredentialsException;
import com.example.user_service.exception.UserNotFoundException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import com.example.user_service.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
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

    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        user = new User();
        user.setId(1L);
        user.setUsername("shubham");
        user.setPassword("encodedPass");
        user.setRole("USER");
        user.setBlacklisted(false);
    }

    // -----------------------------------------------------------------------
    // 1. REGISTER USER
    // -----------------------------------------------------------------------
    @Test
    void testRegisterUser_Success() {
        UserDTO dto = new UserDTO();
        dto.setUsername("shubham");
        dto.setPassword("pass123");

        when(userRepository.findByUsername("shubham")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("pass123")).thenReturn("encodedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);

        ResponseEntity<?> response = userService.registerUser(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> res = (Map<String, Object>) response.getBody();
        assertEquals("User registered successfully", res.get("message"));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_UsernameExists() {
        UserDTO dto = new UserDTO();
        dto.setUsername("shubham");
        dto.setPassword("pass123");

        when(userRepository.findByUsername("shubham"))
                .thenReturn(Optional.of(user));

        ResponseEntity<?> response = userService.registerUser(dto);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(userRepository, never()).save(any());
    }

    // -----------------------------------------------------------------------
    // 2. LOGIN USER
    // -----------------------------------------------------------------------
    @Test
    void testLoginUser_Success() {

        // FIX: Updated to use setters, not constructor
        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("shubham");
        dto.setPassword("password");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);

        when(userRepository.findByUsername("shubham")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()))
                .thenReturn("mockToken");

        ResponseEntity<?> response = userService.loginUser(dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> res = (Map<String, Object>) response.getBody();
        assertEquals("mockToken", res.get("token"));
    }

    @Test
    void testLoginUser_InvalidCredentials() {

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("shubham");
        dto.setPassword("wrongPass");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(InvalidCredentialsException.class,
                () -> userService.loginUser(dto));
    }

    @Test
    void testLoginUser_BlacklistedUser() {

        LoginRequestDTO dto = new LoginRequestDTO();
        dto.setUsername("shubham");
        dto.setPassword("password");

        user.setBlacklisted(true);

        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByUsername("shubham")).thenReturn(Optional.of(user));

        ResponseEntity<?> response = userService.loginUser(dto);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        Map<String, Object> res = (Map<String, Object>) response.getBody();
        assertEquals("USER_BLACKLISTED", res.get("errorCode"));
    }

    // -----------------------------------------------------------------------
    // 3. TOGGLE BLACKLIST
    // -----------------------------------------------------------------------
    @Test
    void testToggleBlacklistStatus() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenReturn(user);

        User updated = userService.toggleBlacklistStatus(1L, true);

        assertTrue(updated.isBlacklisted());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testToggleBlacklistStatus_UserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class,
                () -> userService.toggleBlacklistStatus(1L, true));
    }

    // -----------------------------------------------------------------------
    // 4. GET USER DETAILS
    // -----------------------------------------------------------------------
    @Test
    void testGetUserDetails() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserDetailsDTO dto = userService.getUserDetails(1L);

        assertEquals(1L, dto.getId());
        assertEquals("shubham", dto.getUsername());
        assertFalse(dto.isBlacklisted());
    }

    // -----------------------------------------------------------------------
    // 5. GET USER BY ID (Summary)
    // -----------------------------------------------------------------------
    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserSummaryDTO summary = userService.getUserById(1L);

        assertEquals(1L, summary.getId());
        assertEquals("shubham", summary.getUsername());
    }

    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> userService.getUserById(1L));
    }
}
