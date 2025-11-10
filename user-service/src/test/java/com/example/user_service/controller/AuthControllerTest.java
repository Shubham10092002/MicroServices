package com.example.user_service.controller;

import com.example.user_service.dto.LoginRequestDTO;
import com.example.user_service.dto.UserDTO;
import com.example.user_service.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@ImportAutoConfiguration(exclude = {
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration.class,
        org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
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

    @Nested
    @DisplayName("POST /api/auth/register")
    class RegisterTests {

        @Test
        @DisplayName("should register successfully (200 OK)")
        void register_success() throws Exception {
            UserDTO payload = new UserDTO();
            payload.setUsername("shiv");
            payload.setPassword("strongPass123");
            payload.setRole("USER");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", "User registered successfully");
            responseMap.put("userId", 10L);
            responseMap.put("username", "shiv");
            responseMap.put("role", "USER");

            // ✅ Safe version that bypasses generic inference
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
        void register_validation_missingUsername() throws Exception {
            String json = """
                    { "password": "strongPass123", "role": "USER" }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("should fail validation when password too short")
        void register_validation_shortPassword() throws Exception {
            String json = """
                    { "username": "shiv", "password": "123", "role": "USER" }
                    """;
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginTests {

        @Test
        @DisplayName("should login successfully and return jwt (200 OK)")
        void login_success() throws Exception {
            LoginRequestDTO payload = new LoginRequestDTO();
            payload.setUsername("shiv");
            payload.setPassword("strongPass123");

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
        @DisplayName("should return 401 for invalid credentials")
        void login_invalidCredentials_401() throws Exception {
            LoginRequestDTO payload = new LoginRequestDTO();
            payload.setUsername("shiv");
            payload.setPassword("wrong");

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("errorCode", "INVALID_CREDENTIALS");
            responseMap.put("message", "Incorrect username or password");

            doReturn(ResponseEntity.status(401).body(responseMap))
                    .when(userService)
                    .loginUser(any(LoginRequestDTO.class));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(payload)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                    .andExpect(jsonPath("$.message").value("Incorrect username or password"));
        }

        @Test
        @DisplayName("should return 500 for internal error")
        void login_internalError_500() throws Exception {
            LoginRequestDTO payload = new LoginRequestDTO();
            payload.setUsername("shiv");
            payload.setPassword("strongPass123");

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
                    .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_ERROR"));
        }
    }
}
