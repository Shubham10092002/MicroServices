package com.example.user_service.controller.userController;

import com.example.user_service.dto.UserDetailsDTO;
import com.example.user_service.dto.UserSummaryDTO;
import com.example.user_service.security.UserPrincipal;
import com.example.user_service.service.UserServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserControllerTest {

    @Mock
    private UserServiceImpl userService;

    @InjectMocks
    private UserController userController;

    AutoCloseable mocks;

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    // -------------------------------------------------------
    // Helper method to mock authentication in SecurityContext
    // -------------------------------------------------------
    private void mockAuth(Long userId, String username, String role) {
        UserPrincipal principal = new UserPrincipal(
                userId,
                username,
                role,
                "password123"   // <-- required by your UserPrincipal
        );

        TestingAuthenticationToken auth =
                new TestingAuthenticationToken(principal, null);
        auth.setAuthenticated(true);

        SecurityContextHolder.getContext().setAuthentication(auth);
    }


    // =======================================================
    //                 TEST getUserById()
    // =======================================================

    @Test
    void shouldReturnUserInfoForOwner() {

        mockAuth(1L, "shivam", "USER");

        UserSummaryDTO summary = new UserSummaryDTO(1L, "shivam");
        when(userService.getUserById(1L)).thenReturn(summary);

        var response = userController.getUserById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(summary, response.getBody());
    }

    @Test
    void shouldAllowAdminAccess() {

        mockAuth(99L, "admin", "ADMIN");

        UserSummaryDTO summary = new UserSummaryDTO(1L, "shivam");
        when(userService.getUserById(1L)).thenReturn(summary);

        var response = userController.getUserById(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(summary, response.getBody());
    }

    @Test
    void shouldReturnForbiddenForNonOwner() {

        mockAuth(2L, "ram", "USER");  // User 2 tries to access user 1

        var response = userController.getUserById(1L);

        assertEquals(403, response.getStatusCodeValue());
        assertTrue(((Map<?, ?>) response.getBody()).containsKey("reason"));
    }

    @Test
    void shouldReturn404WhenUserNotFound() {

        mockAuth(1L, "shivam", "USER");

        when(userService.getUserById(1L))
                .thenThrow(new RuntimeException("User not found with ID 1"));

        var response = userController.getUserById(1L);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals("User not found with ID 1",
                ((Map<?, ?>) response.getBody()).get("error"));
    }

    @Test
    void shouldHandleInternalErrors() {

        mockAuth(1L, "shivam", "USER");

        when(userService.getUserById(1L))
                .thenThrow(new RuntimeException("Something went wrong"));

        var response = userController.getUserById(1L);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals("Something went wrong",
                ((Map<?, ?>) response.getBody()).get("error"));
    }

    // =======================================================
    //                 TEST getUserDetails()
    // =======================================================

    @Test
    void shouldReturnDetailsForOwner() {

        mockAuth(1L, "shivam", "USER");

        UserDetailsDTO details = new UserDetailsDTO(1L, "shivam", "USER", false);
        when(userService.getUserDetails(1L)).thenReturn(details);

        var response = userController.getUserDetails(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertEquals(details, response.getBody());
    }

    @Test
    void shouldAllowAdminToGetDetails() {

        mockAuth(99L, "admin", "ADMIN");

        UserDetailsDTO details = new UserDetailsDTO(1L, "shivam", "USER", false);
        when(userService.getUserDetails(1L)).thenReturn(details);

        var response = userController.getUserDetails(1L);

        assertEquals(200, response.getStatusCodeValue());
    }

    @Test
    void shouldReturnForbiddenWhenAccessingAnotherUserDetails() {

        mockAuth(2L, "ram", "USER");  // not admin, not owner

        var response = userController.getUserDetails(1L);

        assertEquals(403, response.getStatusCodeValue());
        assertEquals("ACCESS_DENIED",
                ((Map<?, ?>) response.getBody()).get("errorCode"));
    }

    @Test
    void shouldReturn404WhenUserDetailsNotFound() {

        mockAuth(1L, "shivam", "USER");

        when(userService.getUserDetails(1L)).thenReturn(null);

        var response = userController.getUserDetails(1L);

        assertEquals(404, response.getStatusCodeValue());
        assertEquals("USER_NOT_FOUND",
                ((Map<?, ?>) response.getBody()).get("errorCode"));
    }
}
