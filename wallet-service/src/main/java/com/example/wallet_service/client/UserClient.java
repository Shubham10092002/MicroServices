package com.example.wallet_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.beans.factory.annotation.Value;
import com.example.wallet_service.dto.UserDTO;

@Component
public class UserClient {

    private final RestTemplate restTemplate;
    private final String userServiceBaseUrl;

    public UserClient(RestTemplate restTemplate,
                      @Value("${user.service.url}") String userServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.userServiceBaseUrl = userServiceBaseUrl;
    }

    public UserDTO getUserById(Long userId) {
        String url = userServiceBaseUrl + "/api/auth/" + userId + "/details";
        return restTemplate.getForObject(url, UserDTO.class);
    }
}
