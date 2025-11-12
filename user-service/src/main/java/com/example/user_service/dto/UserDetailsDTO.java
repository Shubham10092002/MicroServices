package com.example.user_service.dto;

public class UserDetailsDTO {
    private Long id;
    private String username;
    private String role;
    private boolean blacklisted;

    public UserDetailsDTO() {}

    public UserDetailsDTO(Long id, String username, String role, boolean blacklisted) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.blacklisted = blacklisted;
    }

    // getters and setters
    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public boolean isBlacklisted() { return blacklisted; }

    public void setId(Long id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setRole(String role) { this.role = role; }
    public void setBlacklisted(boolean blacklisted) { this.blacklisted = blacklisted; }
}
