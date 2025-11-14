package com.example.wallet_service.client.userClient;

import com.example.wallet_service.dto.UserDTO;
import com.example.wallet_service.security.JwtRequestContext;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

@Component
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;
    private final String internalToken = "wallet-service-internal-key"; // same key as above

    public UserClient(RestTemplate restTemplate,
                      @Value("${user.service.url}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    public boolean isUserBlacklisted(Long userId) {
        String url = userServiceBaseUrl + "/internal/users/" + userId + "/status";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-INTERNAL-TOKEN", internalToken);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            return (Boolean) response.getBody().get("blacklisted");
        } catch (Exception e) {
            throw new RuntimeException("Unable to verify user status", e);
        }
    }

    public UserDTO getUserById(Long userId) {
        String url = userServiceBaseUrl + "/api/users/" + userId + "/details";

        String token = JwtRequestContext.getToken();
        if (token == null) {
            throw new RuntimeException("JWT token not available in context");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<UserDTO> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, UserDTO.class
        );

        return response.getBody();
    }
}
