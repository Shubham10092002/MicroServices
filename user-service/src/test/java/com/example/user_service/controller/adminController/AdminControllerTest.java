package com.example.user_service.controller.adminController;

import com.example.user_service.model.User;
import com.example.user_service.service.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testToggleBlacklistToTrue() {
        // Arrange
        Long userId = 1L;
        boolean status = true;

        User user = new User();
        user.setId(userId);
        user.setBlacklisted(status);

        when(userService.toggleBlacklistStatus(userId, status)).thenReturn(user);

        // Act
        ResponseEntity<?> response = adminController.toggleBlacklist(userId, status);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals("User blacklisted successfully", body.get("message"));
        assertEquals(userId, body.get("userId"));
        assertEquals(true, body.get("blacklisted"));

        verify(userService, times(1)).toggleBlacklistStatus(userId, status);
    }

    @Test
    void testToggleBlacklistToFalse() {
        // Arrange
        Long userId = 2L;
        boolean status = false;

        User user = new User();
        user.setId(userId);
        user.setBlacklisted(status);

        when(userService.toggleBlacklistStatus(userId, status)).thenReturn(user);

        // Act
        ResponseEntity<?> response = adminController.toggleBlacklist(userId, status);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        Map<String, Object> body = (Map<String, Object>) response.getBody();

        assertEquals("User removed from blacklist", body.get("message"));
        assertEquals(userId, body.get("userId"));
        assertEquals(false, body.get("blacklisted"));

        verify(userService, times(1)).toggleBlacklistStatus(userId, status);
    }

    @Test
    void testToggleBlacklistThrowsException() {
        // Arrange
        Long userId = 3L;
        boolean status = true;

        when(userService.toggleBlacklistStatus(userId, status))
                .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () ->
                adminController.toggleBlacklist(userId, status));

        assertEquals("User not found", exception.getMessage());
        verify(userService, times(1)).toggleBlacklistStatus(userId, status);
    }
}
