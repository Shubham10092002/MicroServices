package com.example.user_service.controller;

import com.example.user_service.controller.userController.UserController;
import com.example.user_service.dto.UserSummaryDTO;
import com.example.user_service.model.User;
import com.example.user_service.service.UserServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;



@WebMvcTest(
        controllers = UserController.class,
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "com\\.example\\.user_service\\.security\\..*")
        }
)
@ImportAutoConfiguration(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        SecurityAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserServiceImpl userService;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void resetSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    // ----------------------------- TEST CASES --------------------------------------

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetUserByIdTests {

        @Test
        @DisplayName("should return user info when accessed by the same user")
        void shouldReturnUserInfoForSameUser() throws Exception {
            // Arrange
            User loggedInUser = new User();
            loggedInUser.setId(1L);
            loggedInUser.setUsername("shiv");
            loggedInUser.setRole("USER");

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken(loggedInUser, null)
            );

            UserSummaryDTO dto = new UserSummaryDTO(1L, "shiv");
            when(userService.getUserById(1L)).thenReturn(dto);

            // Act & Assert
            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1L))
                    .andExpect(jsonPath("$.username").value("shiv"));
        }

        @Test
        @DisplayName("should allow ADMIN to access any user's data")
        void shouldAllowAdminToAccessAnyUser() throws Exception {
            User adminUser = new User();
            adminUser.setId(99L);
            adminUser.setUsername("admin");
            adminUser.setRole("ADMIN");

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken(adminUser, null)
            );

            UserSummaryDTO dto = new UserSummaryDTO(5L, "otherUser");
            when(userService.getUserById(5L)).thenReturn(dto);

            mockMvc.perform(get("/api/users/5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(5L))
                    .andExpect(jsonPath("$.username").value("otherUser"));
        }

        @Test
        @DisplayName("should return 403 when normal user tries to access another user's profile")
        void shouldReturnForbiddenForUnauthorizedAccess() throws Exception {
            User loggedInUser = new User();
            loggedInUser.setId(1L);
            loggedInUser.setUsername("shiv");
            loggedInUser.setRole("USER");

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken(loggedInUser, null)
            );

            mockMvc.perform(get("/api/users/2"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error").value("Access Denied"))
                    .andExpect(jsonPath("$.reason").value("You cannot access another user's profile."));
        }

        @Test
        @DisplayName("should return 401 when no user is authenticated")
        void shouldReturnUnauthorizedWhenNoUserAuthenticated() throws Exception {
            SecurityContextHolder.clearContext(); // no authentication set

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.error").value("Unauthorized"));
        }





        @Test
        @DisplayName("should return 404 when user not found in service layer")
        void shouldReturnNotFoundWhenUserDoesNotExist() throws Exception {
            User loggedInUser = new User();
            loggedInUser.setId(1L);
            loggedInUser.setUsername("shiv");
            loggedInUser.setRole("USER");

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken(loggedInUser, null)
            );

            when(userService.getUserById(1L))
                    .thenThrow(new RuntimeException("User not found with ID 1"));

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error").value("User not found with ID 1"));
        }




        @Test
        @DisplayName("should handle unexpected internal server errors gracefully")
        void shouldHandleUnexpectedServerError() throws Exception {
            User loggedInUser = new User();
            loggedInUser.setId(1L);
            loggedInUser.setUsername("shiv");
            loggedInUser.setRole("ADMIN");

            SecurityContextHolder.getContext().setAuthentication(
                    new TestingAuthenticationToken(loggedInUser, null)
            );

            when(userService.getUserById(anyLong()))
                    .thenThrow(new RuntimeException("Database failure"));

            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isNotFound()) // your controller catches runtime exceptions as 404
                    .andExpect(jsonPath("$.error").value("Database failure"));
        }
    }
}