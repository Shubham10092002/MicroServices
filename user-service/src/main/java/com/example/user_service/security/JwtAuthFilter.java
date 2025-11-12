package com.example.user_service.security;

import com.example.user_service.exception.JwtValidationException;
import com.example.user_service.model.User;
import com.example.user_service.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;


    public JwtAuthFilter(JwtUtil jwtUtil, UserRepository userRepository) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        try {
            //  Step 1: Extract JWT token from Authorization header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                //  Step 2: Validate JWT
                if (jwtUtil.isTokenValid(token)) {

                    String username = jwtUtil.extractUsername(token);
                    String role = jwtUtil.extractRole(token);

                    //  Step 3: Fetch full user from DB
                    User user = userRepository.findByUsername(username).orElse(null);

                    if (user != null) {
                        //  Check blacklist
                        if (user.isBlacklisted()) {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write(
                                    "{\"errorCode\":\"USER_BLACKLISTED\",\"message\":\"Your account has been blacklisted. Access denied.\"}");
                            return;
                        }

                        //  Step 4: Create Spring Security authentication
                        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(user, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }

            //  Continue filter chain if all checks pass
            filterChain.doFilter(request, response);

        } catch (JwtValidationException ex) {
            //  Step 5: Handle invalid/expired/malformed tokens cleanly
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                    "{\"errorCode\":\"%s\",\"message\":\"%s\"}",
                    ex.getErrorCode(),
                    ex.getMessage()
            ));
        }
    }
}
