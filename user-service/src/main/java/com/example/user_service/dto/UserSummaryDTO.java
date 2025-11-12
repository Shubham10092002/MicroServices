package com.example.user_service.dto;

import java.math.BigDecimal;

public class UserSummaryDTO {

    private Long id;
    private String username;

    public UserSummaryDTO() {}

    public UserSummaryDTO(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
