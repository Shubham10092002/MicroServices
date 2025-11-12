package com.example.user_service.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");

        String jsonResponse = String.format("""
    {
      "timestamp": "%s",
      "status": 403,
      "errorCode": "ACCESS_DENIED",
      "message": "You are not authorized to perform this action."
    }
""", java.time.LocalDateTime.now());


        response.getWriter().write(jsonResponse);
    }
}
