package com.example.user_service.controller.publicController;

import com.example.user_service.exception.GlobalExceptionHandler;
//import org.springframework.boot.test.mock.mockito.MockitoBean;

import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = AuthController.class,
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
@Import(GlobalExceptionHandler.class)  // ✅ ADD THIS LINE

class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private String toJson(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    // ---------------- REGISTER TESTS ----------------
    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register successfully (200 OK)")
        void register_success() throws Exception {
            UserDTO payload = new UserDTO();
            payload.setUsername("shiv");
            payload.setPassword("StrongPass123");
            payload.setRole("USER");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", "User registered successfully");
            responseMap.put("userId", 10L);
            responseMap.put("username", "shiv");
            responseMap.put("role", "USER");

            doReturn(ResponseEntity.ok(responseMap))
                    .when(userService)
                    .registerUser(any(UserDTO.class));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("User registered successfully"))
                    .andExpect(jsonPath("$.userId").value(10L))
                    .andExpect(jsonPath("$.username").value("shiv"))
                    .andExpect(jsonPath("$.role").value("USER"));

            verify(userService).registerUser(any(UserDTO.class));
        }

        @Test
        @DisplayName("should fail validation when username missing")
        void register_missingUsername() throws Exception {
            String json = """
                    { "password": "StrongPass123", "role": "USER" }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail validation when password too short")
        void register_shortPassword() throws Exception {
            String json = """
                    { "username": "shiv", "password": "12", "role": "USER" }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------------- LOGIN TESTS ----------------
    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return jwt (200 OK)")
        void login_success() throws Exception {
            LoginRequestDTO payload = new LoginRequestDTO();
            payload.setUsername("shiv");
            payload.setPassword("StrongPass123");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("token", "mock-jwt");
            responseMap.put("userId", 1L);
            responseMap.put("username", "shiv");
            responseMap.put("role", "USER");

            doReturn(ResponseEntity.ok(responseMap))
                    .when(userService)
                    .loginUser(any(LoginRequestDTO.class));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.token").value("mock-jwt"))
                    .andExpect(jsonPath("$.username").value("shiv"))
                    .andExpect(jsonPath("$.role").value("USER"));
        }






        @Test
        @DisplayName("should return 500 for internal authentication error")
        void login_internalServerError() throws Exception {
            LoginRequestDTO payload = new LoginRequestDTO();
            payload.setUsername("shiv");
            payload.setPassword("StrongPass123");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("errorCode", "AUTHENTICATION_ERROR");
            responseMap.put("message", "Login failed due to internal error");

            doReturn(ResponseEntity.internalServerError().body(responseMap))
                    .when(userService)
                    .loginUser(any(LoginRequestDTO.class));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(payload)))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_ERROR"))
                    .andExpect(jsonPath("$.message").value("Login failed due to internal error"));
        }
    }
}
