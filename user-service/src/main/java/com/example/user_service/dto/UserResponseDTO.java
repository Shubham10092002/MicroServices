package com.example.user_service.dto;

import com.example.user_service.model.User;

import java.math.BigDecimal;

public class UserResponseDTO {

    private Long userId;
    private String username;
    private Long walletId;
    private String walletName;
    private BigDecimal walletBalance;

    public UserResponseDTO() {}

    public UserResponseDTO(User user, Wallet wallet) {
        this.userId = user.getId();
        this.username = user.getUsername();
        if (wallet != null) {
            this.walletId = wallet.getId();
            this.walletName = wallet.getWalletName();
            this.walletBalance = wallet.getBalance();
        }
    }

    // Getters & Setters

    public String getWalletName() {
        return walletName;
    }

    public void setWalletName(String walletName) {
        this.walletName = walletName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Long getWalletId() {
        return walletId;
    }

    public void setWalletId(Long walletId) {
        this.walletId = walletId;
    }

    public BigDecimal getWalletBalance() {
        return walletBalance;
    }

    public void setWalletBalance(BigDecimal walletBalance) {
        this.walletBalance = walletBalance;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
// ...

}
